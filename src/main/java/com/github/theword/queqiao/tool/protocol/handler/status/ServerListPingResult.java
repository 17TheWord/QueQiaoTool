package com.github.theword.queqiao.tool.protocol.handler.status;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerListPingResult {
    private static final String FIELD_AVAILABLE = "available";
    private static final String FIELD_HOST = "host";
    private static final String FIELD_PORT = "port";
    private static final String FIELD_REASON = "reason";
    private static final String FIELD_ERROR = "error";

    private final boolean available;
    private final String host;
    private final int port;
    private final String reason;
    private final String error;
    private final MinecraftPingResponse pingResponse;

    private ServerListPingResult(boolean available, String host, int port, String reason, String error, MinecraftPingResponse pingResponse) {
        this.available = available;
        this.host = host;
        this.port = port;
        this.reason = reason;
        this.error = error;
        this.pingResponse = pingResponse;
    }

    public static ServerListPingResult of(boolean available, String host, int port, String reason, String error, MinecraftPingResponse pingResponse) {
        return new ServerListPingResult(available, host, port, reason, error, pingResponse);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put(FIELD_AVAILABLE, available);
        result.put(FIELD_HOST, host);
        result.put(FIELD_PORT, port);
        result.put(FIELD_REASON, reason);
        result.put(FIELD_ERROR, error);
        if (pingResponse != null) {
            result.putAll(pingResponse.toMap());
        }
        return result;
    }
}
