package com.github.theword.queqiao.tool.protocol.handler.status;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.exception.status.MinecraftPingException;
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

    private static final Path[] REGEX_CONFIG_CANDIDATES = new Path[]{
            Paths.get(CONFIG_DIRECTORY, BaseConstant.MODULE_NAME, REGEX_CONFIG_FILE),
            Paths.get(BaseConstant.MODULE_NAME, REGEX_CONFIG_FILE)
    };

    private static final MinecraftPingClient PING_CLIENT = new MinecraftPingClient();
    private static final SystemMetricsCollector METRICS = new SystemMetricsCollector();

    private static volatile PingTarget pingTarget = PingTarget.unavailable(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);

    private ServerStatusCollector() {
    }

    private static final class PingTarget {
        private final String host;
        private final int port;
        private final boolean available;

        private PingTarget(String host, int port, boolean available) {
            this.host = host;
            this.port = port;
            this.available = available;
        }

        private static PingTarget unavailable(String host, int port) {
            return new PingTarget(host, port, false);
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
        pingTarget = new PingTarget(host, port, available);
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

    public static Map<String, Object> collectStatusSnapshot() {
        ServerStatusSnapshot snapshot = new ServerStatusSnapshot(
                GlobalContext.getServerType(),
                GlobalContext.getServerVersion(),
                collectServerListPing(),
                METRICS.collectCpuInformation(),
                METRICS.collectMemoryInformation()
        );
        return snapshot.toMap();
    }

    private static ServerListPingResult collectServerListPing() {
        PingTarget currentTarget = pingTarget;
        if (!currentTarget.available) {
            return ServerListPingResult.of(false, currentTarget.host, currentTarget.port, PING_REASON_NOT_CONFIGURED, null, null);
        }

        try {
            MinecraftPingResponse pingResponse = PING_CLIENT.fetchStatus(currentTarget.host, currentTarget.port);
            return ServerListPingResult.of(true, currentTarget.host, currentTarget.port, PING_REASON_OK, null, pingResponse);
        } catch (MinecraftPingException e) {
            String reason = resolvePingFailureReason(e);
            String error = resolvePingErrorMessage(e);
            Logger logger = GlobalContext.getLogger();
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
