package io.github.theword.queqiao.core.protocol.handler.status;

import io.github.theword.queqiao.core.GlobalContext;
import io.github.theword.queqiao.core.constant.BaseConstant;
import io.github.theword.queqiao.core.exception.status.MinecraftPingException;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 服务器状态采集工具。
 */
public final class ServerStatusCollector {
    private static final int DEFAULT_SERVER_PORT = 25565;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final String SERVER_PROPERTIES_FILE = "server.properties";
    private static final String REGEX_CONFIG_FILE = "regex.yml";
    private static final String CONFIG_DIRECTORY = "config";
    private static final String LOGS_DIRECTORY = "logs";
    private static final String LOG_PATH_KEY = "log_path";
    private static final String SERVER_IP_KEY = "server-ip";
    private static final String SERVER_PORT_KEY = "server-port";
    private static final String PING_REASON_NOT_CONFIGURED = "not_configured";
    private static final String PING_REASON_OK = "ok";
    private static final String PING_REASON_TIMEOUT = "timeout";
    private static final String PING_REASON_OFFLINE = "offline";
    private static final String PING_REASON_ERROR = "error";
    private static final int MIN_REFRESH_INTERVAL_SECONDS = 5;
    private static final int DEFAULT_REFRESH_INTERVAL_SECONDS = 10;
    private static final long INITIAL_SNAPSHOT_WAIT_MILLIS = 7000L;

    private static final Path[] REGEX_CONFIG_CANDIDATES = new Path[]{
            Paths.get(CONFIG_DIRECTORY, BaseConstant.MODULE_NAME, REGEX_CONFIG_FILE),
            Paths.get(BaseConstant.MODULE_NAME, REGEX_CONFIG_FILE)
    };

    private static final MinecraftPingClient PING_CLIENT = new MinecraftPingClient();
    private static volatile SystemMetricsCollector metrics = new SystemMetricsCollector(null);

    /** 未启动 Runtime 时的兼容缓存有效期。 */
    private static final long SNAPSHOT_CACHE_TTL_MILLIS = 2000L;
    private static final Object LIFECYCLE_LOCK = new Object();
    private static final AtomicLong TARGET_VERSION = new AtomicLong();

    private static volatile SnapshotCache snapshotCache;
    private static volatile PingTarget pingTarget = PingTarget.unavailable(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    private static volatile ScheduledThreadPoolExecutor refreshExecutor;
    private static volatile ScheduledFuture<?> refreshTask;
    private static volatile int refreshIntervalSeconds = DEFAULT_REFRESH_INTERVAL_SECONDS;
    private static volatile Logger statusLogger;
    private static volatile SnapshotWaiter snapshotWaiter = new SnapshotWaiter(pingTarget);

    private ServerStatusCollector() {
    }

    private static final class PingTarget {
        private final String host;
        private final int port;
        private final boolean available;
        private final long version;

        private PingTarget(String host, int port, boolean available, long version) {
            this.host = host;
            this.port = port;
            this.available = available;
            this.version = version;
        }

        private static PingTarget unavailable(String host, int port) {
            return new PingTarget(host, port, false, TARGET_VERSION.incrementAndGet());
        }

        private boolean sameEndpoint(String otherHost, int otherPort, boolean otherAvailable) {
            return port == otherPort && available == otherAvailable && host.equals(otherHost);
        }
    }

    /**
     * 启动状态快照定时采集。Runtime 启动完成后调用；重复调用只重设现有任务。
     */
    public static void startRefreshScheduler(int intervalSeconds, Logger logger) {
        synchronized (LIFECYCLE_LOCK) {
            statusLogger = logger;
            refreshIntervalSeconds = Math.max(MIN_REFRESH_INTERVAL_SECONDS, intervalSeconds);
            if (refreshExecutor == null || refreshExecutor.isShutdown()) {
                ThreadFactory threadFactory = runnable -> {
                    Thread thread = new Thread(runnable, "QueQiao-Status-Collector");
                    thread.setDaemon(true);
                    return thread;
                };
                refreshExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
                refreshExecutor.setRemoveOnCancelPolicy(true);
                metrics = new SystemMetricsCollector(logger);
                snapshotCache = null;
                snapshotWaiter = new SnapshotWaiter(pingTarget);
            }
            scheduleRefreshLocked(0L);
        }
    }

    /** 更新间隔；未变化时不打断当前采集周期。 */
    public static void updateRefreshInterval(int intervalSeconds) {
        synchronized (LIFECYCLE_LOCK) {
            int normalizedInterval = Math.max(MIN_REFRESH_INTERVAL_SECONDS, intervalSeconds);
            if (refreshIntervalSeconds == normalizedInterval) {
                return;
            }
            refreshIntervalSeconds = normalizedInterval;
            if (refreshExecutor != null && !refreshExecutor.isShutdown()) {
                long initialDelay = snapshotCache == null ? 0L : normalizedInterval;
                scheduleRefreshLocked(initialDelay);
            }
        }
    }

    /** 关闭 Runtime 持有的状态采集任务与线程。 */
    public static void stopRefreshScheduler() {
        synchronized (LIFECYCLE_LOCK) {
            SnapshotWaiter waiter = snapshotWaiter;
            waiter.result.completeExceptionally(new IllegalStateException("状态采集器已停止"));
            ScheduledFuture<?> task = refreshTask;
            refreshTask = null;
            if (task != null) {
                task.cancel(true);
            }
            ScheduledThreadPoolExecutor executor = refreshExecutor;
            refreshExecutor = null;
            if (executor != null) {
                executor.shutdownNow();
            }
            snapshotCache = null;
            statusLogger = null;
            metrics = new SystemMetricsCollector(null);
            snapshotWaiter = new SnapshotWaiter(pingTarget);
        }
    }

    private static void scheduleRefreshLocked(long initialDelaySeconds) {
        ScheduledFuture<?> current = refreshTask;
        if (current != null) {
            current.cancel(false);
        }
        ScheduledThreadPoolExecutor executor = refreshExecutor;
        if (executor != null && !executor.isShutdown()) {
            refreshTask = executor.scheduleWithFixedDelay(
                    ServerStatusCollector::refreshAndPublish,
                    initialDelaySeconds,
                    refreshIntervalSeconds,
                    TimeUnit.SECONDS);
        }
    }

    public static void initPingTarget(Logger logger) {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path serverPropertiesPath = resolveServerPropertiesPath(workingDirectory, logger);
        initPingTarget(serverPropertiesPath, logger);
    }

    static Path resolveServerPropertiesPath(Path workingDirectory, Logger logger) {
        Path absoluteWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        for (Path relativeRegexPath : REGEX_CONFIG_CANDIDATES) {
            Path regexConfigPath = absoluteWorkingDirectory.resolve(relativeRegexPath).normalize();
            if (!Files.isRegularFile(regexConfigPath)) {
                continue;
            }

            String logPath = readLogPath(regexConfigPath, logger);
            if (logPath == null || logPath.trim().isEmpty()) {
                continue;
            }

            Path serverRoot = inferServerRoot(regexConfigPath, logPath, absoluteWorkingDirectory, logger);
            if (serverRoot == null) {
                continue;
            }

            return serverRoot.resolve(SERVER_PROPERTIES_FILE).normalize();
        }

        return absoluteWorkingDirectory.resolve(SERVER_PROPERTIES_FILE).normalize();
    }

    private static String readLogPath(Path regexConfigPath, Logger logger) {
        try (InputStream inputStream = Files.newInputStream(regexConfigPath)) {
            Yaml yaml = new Yaml();
            Object yamlObject = yaml.load(inputStream);
            if (!(yamlObject instanceof Map)) {
                logger.warn("regex.yml 内容不是 Map 结构，无法读取 log_path：{}", regexConfigPath);
                return null;
            }

            Object logPathObject = ((Map<?, ?>) yamlObject).get(LOG_PATH_KEY);
            if (!(logPathObject instanceof String)) {
                logger.warn("regex.yml 未配置 log_path，无法获取 server.properties：{}", regexConfigPath);
                return null;
            }

            String logPath = ((String) logPathObject).trim();
            if (logPath.isEmpty()) {
                logger.warn("regex.yml 的 log_path 为空，无法获取 server.properties：{}", regexConfigPath);
                return null;
            }
            return logPath;
        } catch (Exception e) {
            logger.warn("读取 regex.yml 失败，无法获取 server.properties：{}，错误：{}", regexConfigPath, e.getMessage());
            return null;
        }
    }

    private static Path inferServerRoot(Path regexConfigPath, String logPath, Path workingDirectory, Logger logger) {
        final Path logPathObject;
        try {
            logPathObject = Paths.get(logPath);
        } catch (Exception e) {
            logger.warn("log_path 非法，无法获取 server.properties：{}，错误：{}", logPath, e.getMessage());
            return null;
        }

        if (logPathObject.isAbsolute()) {
            return extractServerRootFromLogPath(logPathObject.normalize());
        }

        Path regexBasedRoot = deriveRootFromRegexConfig(regexConfigPath);
        Path[] rootCandidates = new Path[]{regexBasedRoot, workingDirectory};

        Path firstInferredRoot = null;
        for (Path rootCandidate : rootCandidates) {
            if (rootCandidate == null) {
                continue;
            }

            Path resolvedLogPath = rootCandidate.resolve(logPathObject).normalize();
            Path inferredRoot = extractServerRootFromLogPath(resolvedLogPath);
            if (inferredRoot == null) {
                continue;
            }
            if (firstInferredRoot == null) {
                firstInferredRoot = inferredRoot;
            }

            Path serverPropertiesPath = inferredRoot.resolve(SERVER_PROPERTIES_FILE).normalize();
            if (Files.isRegularFile(serverPropertiesPath)) {
                return inferredRoot;
            }
        }
        return firstInferredRoot;
    }

    private static Path deriveRootFromRegexConfig(Path regexConfigPath) {
        Path normalizedPath = regexConfigPath.toAbsolutePath().normalize();
        Path moduleDirectory = normalizedPath.getParent();
        if (moduleDirectory == null) {
            return null;
        }

        Path moduleParent = moduleDirectory.getParent();
        if (moduleParent == null) {
            return null;
        }

        if (equalsIgnoreCase(moduleDirectory.getFileName(), BaseConstant.MODULE_NAME) && equalsIgnoreCase(moduleParent.getFileName(), CONFIG_DIRECTORY)) {
            return moduleParent.getParent();
        }

        return moduleParent;
    }

    private static boolean equalsIgnoreCase(Path pathSegment, String text) {
        return pathSegment != null && pathSegment.toString().equalsIgnoreCase(text);
    }

    private static Path extractServerRootFromLogPath(Path resolvedLogPath) {
        Path logDirectory = resolvedLogPath.getParent();
        if (logDirectory == null) {
            return null;
        }

        if (equalsIgnoreCase(logDirectory.getFileName(), LOGS_DIRECTORY) && logDirectory.getParent() != null) {
            return logDirectory.getParent();
        }

        return logDirectory;
    }

    static void initPingTarget(Path serverPropertiesPath, Logger logger) {
        String host = DEFAULT_SERVER_HOST;
        int port = DEFAULT_SERVER_PORT;
        Path normalizedPath = serverPropertiesPath.toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalizedPath)) {
            setPingTarget(host, port, true);
            return;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(normalizedPath)) {
            properties.load(inputStream);
            String configuredHost = properties.getProperty(SERVER_IP_KEY, "").trim();
            String configuredPort = properties.getProperty(SERVER_PORT_KEY, String.valueOf(DEFAULT_SERVER_PORT)).trim();

            if (!configuredHost.isEmpty()) {
                host = configuredHost;
            }
            port = parsePort(configuredPort, logger);
        } catch (IOException e) {
            logger.warn("读取 server.properties 失败，状态接口将使用默认地址 {}:{}，错误：{}", host, port, e.getMessage());
            setPingTarget(host, port, false);
            return;
        }

        setPingTarget(host, port, true);
    }

    private static void setPingTarget(String host, int port, boolean available) {
        synchronized (LIFECYCLE_LOCK) {
            PingTarget current = pingTarget;
            if (current.sameEndpoint(host, port, available)) {
                return;
            }

            PingTarget replacement = new PingTarget(host, port, available, TARGET_VERSION.incrementAndGet());
            pingTarget = replacement;
            snapshotCache = null;
            snapshotWaiter.result.completeExceptionally(
                    new java.util.concurrent.CancellationException("Ping target changed"));
            snapshotWaiter = new SnapshotWaiter(replacement);
            // 目标变化后立即刷新，不等待原来的周期；旧刷新结果会因 target version 不匹配而被丢弃。
            if (refreshExecutor != null && !refreshExecutor.isShutdown()) {
                scheduleRefreshLocked(0L);
            }
        }
    }

    private static int parsePort(String portText, Logger logger) {
        try {
            int parsedPort = Integer.parseInt(portText);
            if (parsedPort <= 0 || parsedPort > 65535) {
                logger.warn("server-port 配置越界（{}），将使用默认端口 {}", portText, DEFAULT_SERVER_PORT);
                return DEFAULT_SERVER_PORT;
            }
            return parsedPort;
        } catch (NumberFormatException e) {
            logger.warn("server-port 配置非法（{}），将使用默认端口 {}", portText, DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }
    }

    /**
     * 返回最近一次定时采集的完整状态快照。
     *
     * <p>Runtime 正常运行时，此方法不执行网络 Ping 或系统指标采集。只有首次采集尚未完成时，
     * 调用方才等待共享的首轮结果，等待有明确上限。Runtime 未启动时保留同步回退，便于兼容
     * 直接使用该静态门面的既有调用。
     *
     * @return 状态快照 Map
     */
    public static Map<String, Object> collectStatusSnapshot() {
        return getOrCollectSnapshot().toMap();
    }

    private static ServerStatusSnapshot getOrCollectSnapshot() {
        ScheduledThreadPoolExecutor executor = refreshExecutor;
        if (executor != null && !executor.isShutdown()) {
            for (int attempt = 0; attempt < 3; attempt++) {
                PingTarget target = pingTarget;
                SnapshotCache cached = snapshotCache;
                if (cached != null && cached.targetVersion == target.version) {
                    return cached.snapshot;
                }

                SnapshotWaiter waiter = snapshotWaiter;
                if (waiter.target.version != target.version) {
                    continue;
                }
                try {
                    SnapshotCache firstSnapshot = waiter.result.get(
                            INITIAL_SNAPSHOT_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                    if (firstSnapshot.targetVersion == pingTarget.version) {
                        return firstSnapshot.snapshot;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return createSnapshot(pingTarget, timeoutPingResult(pingTarget));
                } catch (ExecutionException | TimeoutException e) {
                    return createSnapshot(pingTarget, timeoutPingResult(pingTarget));
                }
            }
            PingTarget target = pingTarget;
            return createSnapshot(target, timeoutPingResult(target));
        }

        PingTarget target = pingTarget;
        SnapshotCache cached = snapshotCache;
        if (cached != null && cached.targetVersion == target.version
                && !cached.isExpired(System.nanoTime())) {
            return cached.snapshot;
        }

        SnapshotCache fresh = collectSnapshot(target);
        if (target == pingTarget) {
            snapshotCache = fresh;
        }
        return fresh.snapshot;
    }

    private static void refreshAndPublish() {
        ScheduledThreadPoolExecutor executor = refreshExecutor;
        PingTarget target = pingTarget;
        SnapshotWaiter waiter = snapshotWaiter;
        try {
            SnapshotCache fresh = collectSnapshot(target);
            synchronized (LIFECYCLE_LOCK) {
                if (executor != null && executor == refreshExecutor && !executor.isShutdown()
                        && target == pingTarget) {
                    snapshotCache = fresh;
                    if (waiter == snapshotWaiter && waiter.target == target) {
                        waiter.result.complete(fresh);
                    }
                }
            }
        } catch (RuntimeException e) {
            Logger logger = statusLogger;
            if (logger != null) {
                logger.error("采集服务器状态快照失败，保留当前缓存", e);
            }
            if (waiter.target == target) {
                waiter.result.completeExceptionally(e);
            }
        }
    }

    private static SnapshotCache collectSnapshot(PingTarget target) {
        ServerStatusSnapshot snapshot = createSnapshot(target, collectServerListPing(target));
        return new SnapshotCache(snapshot, System.nanoTime(), target.version);
    }

    private static ServerStatusSnapshot createSnapshot(PingTarget target, ServerListPingResult pingResult) {
        return new ServerStatusSnapshot(
                GlobalContext.getServerType(),
                GlobalContext.getServerVersion(),
                pingResult,
                metrics.collectCpuInformation(),
                metrics.collectMemoryInformation());
    }

    private static ServerListPingResult timeoutPingResult(PingTarget target) {
        return ServerListPingResult.of(
                target.available, target.host, target.port, PING_REASON_TIMEOUT,
                "等待状态采集超时", null);
    }

    /** 快照、单调时间和探测目标版本一起发布，读取时三者保持一致。 */
    private static final class SnapshotCache {

        private final ServerStatusSnapshot snapshot;
        private final long createdAtNanos;
        private final long targetVersion;

        private SnapshotCache(ServerStatusSnapshot snapshot, long createdAtNanos, long targetVersion) {
            this.snapshot = snapshot;
            this.createdAtNanos = createdAtNanos;
            this.targetVersion = targetVersion;
        }

        private boolean isExpired(long nowNanos) {
            return nowNanos - createdAtNanos >= TimeUnit.MILLISECONDS.toNanos(SNAPSHOT_CACHE_TTL_MILLIS);
        }
    }

    private static final class SnapshotWaiter {
        private final PingTarget target;
        private final CompletableFuture<SnapshotCache> result = new CompletableFuture<>();

        private SnapshotWaiter(PingTarget target) {
            this.target = target;
        }
    }

    private static ServerListPingResult collectServerListPing(PingTarget currentTarget) {
        if (!currentTarget.available) {
            return ServerListPingResult.of(false, currentTarget.host, currentTarget.port, PING_REASON_NOT_CONFIGURED, null, null);
        }

        try {
            MinecraftPingResponse pingResponse = PING_CLIENT.fetchStatus(currentTarget.host, currentTarget.port);
            return ServerListPingResult.of(true, currentTarget.host, currentTarget.port, PING_REASON_OK, null, pingResponse);
        } catch (MinecraftPingException e) {
            String reason = resolvePingFailureReason(e);
            String error = resolvePingErrorMessage(e);
            Logger logger = statusLogger != null ? statusLogger : GlobalContext.getLogger();
            if (logger != null) {
                logger.warn("Minecraft Server List Ping failed, reason={}, host={}, port={}, error={}", reason, currentTarget.host, currentTarget.port, error);
            }
            return ServerListPingResult.of(true, currentTarget.host, currentTarget.port, reason, error, null);
        }
    }

    private static String resolvePingFailureReason(MinecraftPingException exception) {
        Throwable cause = rootCause(exception);
        if (cause instanceof SocketTimeoutException) {
            return PING_REASON_TIMEOUT;
        }
        if (cause instanceof ConnectException || cause instanceof UnknownHostException || cause instanceof NoRouteToHostException || cause instanceof UnresolvedAddressException) {
            return PING_REASON_OFFLINE;
        }
        return PING_REASON_ERROR;
    }

    private static String resolvePingErrorMessage(MinecraftPingException exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }
}
