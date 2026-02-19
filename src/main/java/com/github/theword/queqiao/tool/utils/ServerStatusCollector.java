package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import org.slf4j.Logger;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 服务状态采集工具。
 *
 * <p>用于 {@code get_status} 接口，包含以下能力：</p>
 * <p>1. 读取缓存的 server.properties 主机与端口</p>
 * <p>2. 执行一次 Minecraft Server List Ping</p>
 * <p>3. 采集 CPU 与内存信息并组装返回数据</p>
 */
public final class ServerStatusCollector {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 25565;

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
     * 从缓存工具读取目标地址并更新状态探测目标。
     *
     * @param logger 日志实现
     */
    public static void initPingTarget(Logger logger) {
        ServerPropertiesTool.refresh();

        String host = ServerPropertiesTool.getValue("server-ip");
        if (isBlank(host)) {
            host = DEFAULT_SERVER_HOST;
        }

        String portText = ServerPropertiesTool.getValue("server-port");
        int port = parsePort(portText, logger);

        setPingTarget(host, port, true);
    }

    private static int parsePort(String portText, Logger logger) {
        if (isBlank(portText)) {
            return DEFAULT_SERVER_PORT;
        }

        try {
            int parsedPort = Integer.parseInt(portText);
            if (parsedPort > 0 && parsedPort <= 65535) {
                return parsedPort;
            }
        } catch (NumberFormatException ignored) {
        }
        logger.warn("server-port 配置非法（{}），将使用默认端口 {}", portText, DEFAULT_SERVER_PORT);
        return DEFAULT_SERVER_PORT;
    }

    private static void setPingTarget(String host, int port, boolean available) {
        pingTarget = new PingTarget(host, port, available);
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
                logger.warn(
                        "Minecraft Server List Ping failed, reason={}, host={}, port={}, error={}", reason, currentTarget.host, currentTarget.port, error);
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

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Map<String, Object> collectCpuInformation() {
        return METRICS.collectCpuInformation();
    }

    private static Map<String, Object> collectMemoryInformation() {
        return METRICS.collectMemoryInformation();
    }
}
