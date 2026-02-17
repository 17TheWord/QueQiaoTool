package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 服务器状态采集工具
 *
 * <p>用于 {@code get_status} 接口，包含以下能力：</p>
 * <p>1. 启动时读取并缓存 {@code server.properties} 的服务器地址</p>
 * <p>2. 接口调用时执行一次 Minecraft Server List Ping</p>
 * <p>3. 采集 CPU 与内存信息并组装返回数据</p>
 */
public final class ServerStatusCollector {
    private static final int DEFAULT_SERVER_PORT = 25565;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final Path[] REGEX_CONFIG_CANDIDATES = new Path[]{Paths.get("config", BaseConstant.MODULE_NAME, "regex.yml"), Paths.get(BaseConstant.MODULE_NAME, "regex.yml")
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

    /**
     * 读取并缓存 Minecraft 服务器地址
     *
     * <p>优先从 {@code ./config/QueQiao/regex.yml} 的 {@code log_path} 获取服务端根目录，</p>
     * <p>再读取该目录下的 {@code server.properties}</p>
     *
     * @param logger 日志实现
     */
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

            Path serverPropertiesPath = serverRoot.resolve("server.properties").normalize();
            return serverPropertiesPath;
        }

        Path fallbackPath = absoluteWorkingDirectory.resolve("server.properties").normalize();
        return fallbackPath;
    }

    private static String readLogPath(Path regexConfigPath, Logger logger) {
        try (InputStream inputStream = Files.newInputStream(regexConfigPath)) {
            Yaml yaml = new Yaml();
            Object yamlObject = yaml.load(inputStream);
            if (!(yamlObject instanceof Map)) {
                logger.warn("regex.yml 内容不是 Map 结构，无法读取 log_path：{}", regexConfigPath);
                return null;
            }

            Object logPathObject = ((Map<?, ?>) yamlObject).get("log_path");
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

            Path serverPropertiesPath = inferredRoot.resolve("server.properties").normalize();
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

        if (equalsIgnoreCase(moduleDirectory.getFileName(), BaseConstant.MODULE_NAME) && equalsIgnoreCase(moduleParent.getFileName(), "config")) {
            Path serverRoot = moduleParent.getParent();
            if (serverRoot != null) {
                return serverRoot;
            }
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

        if (equalsIgnoreCase(logDirectory.getFileName(), "logs") && logDirectory.getParent() != null) {
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
            String configuredHost = properties.getProperty("server-ip", "").trim();
            String configuredPort = properties.getProperty("server-port", String.valueOf(DEFAULT_SERVER_PORT)).trim();

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

    /**
     * 采集一次 {@code get_status} 响应数据
     *
     * @return 接口 data 字段
     */
    public static Map<String, Object> collectStatusSnapshot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("server_type", GlobalContext.getServerType());
        data.put("server_version", GlobalContext.getServerVersion());
        data.put("server_list_ping", collectServerListPing());
        data.put("cpu_information", collectCpuInformation());
        data.put("memory_information", collectMemoryInformation());
        return data;
    }

    private static Map<String, Object> collectServerListPing() {
        PingTarget currentTarget = pingTarget;
        if (!currentTarget.available) {
            return buildPingResult(currentTarget, false, "not_configured", null, null);
        }

        try {
            Map<String, Object> pingData = PING_CLIENT.fetchStatus(currentTarget.host, currentTarget.port);
            return buildPingResult(currentTarget, true, "ok", null, pingData);
        } catch (Exception e) {
            String reason = resolvePingFailureReason(e);
            String error = resolvePingErrorMessage(e);
            Logger logger = GlobalContext.getLogger();
            if (logger != null) {
                logger.warn("Minecraft Server List Ping failed, reason={}, host={}, port={}, error={}", reason, currentTarget.host, currentTarget.port, error);
            }
            return buildPingResult(currentTarget, true, reason, error, null);
        }
    }

    private static Map<String, Object> buildPingResult(PingTarget target, boolean available, String reason, String error, Map<String, Object> pingData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("available", available);
        result.put("host", target.host);
        result.put("port", target.port);
        result.put("reason", reason);
        result.put("error", error);
        if (pingData != null) {
            result.putAll(pingData);
        }
        return result;
    }

    private static String resolvePingFailureReason(Exception exception) {
        if (exception instanceof SocketTimeoutException) {
            return "timeout";
        }
        if (exception instanceof ConnectException || exception instanceof UnknownHostException || exception instanceof NoRouteToHostException || exception instanceof UnresolvedAddressException) {
            return "offline";
        }
        return "error";
    }

    private static String resolvePingErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }

    private static Map<String, Object> collectCpuInformation() {
        return METRICS.collectCpuInformation();
    }

    private static Map<String, Object> collectMemoryInformation() {
        return METRICS.collectMemoryInformation();
    }
}
