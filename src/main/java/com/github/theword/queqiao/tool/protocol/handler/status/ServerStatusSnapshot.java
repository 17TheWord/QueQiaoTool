package com.github.theword.queqiao.tool.protocol.handler.status;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ServerStatusSnapshot {
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_SERVER_TYPE = "server_type";
    private static final String FIELD_SERVER_VERSION = "server_version";
    private static final String FIELD_SERVER_LIST_PING = "server_list_ping";
    private static final String FIELD_CPU_INFORMATION = "cpu_information";
    private static final String FIELD_MEMORY_INFORMATION = "memory_information";

    private final long timestamp;
    private final String serverType;
    private final String serverVersion;
    private final ServerListPingResult serverListPing;
    private final Map<String, Object> cpuInformation;
    private final Map<String, Object> memoryInformation;

    public ServerStatusSnapshot(String serverType, String serverVersion, ServerListPingResult serverListPing, Map<String, Object> cpuInformation, Map<String, Object> memoryInformation) {
        this.timestamp = System.currentTimeMillis();
        this.serverType = serverType;
        this.serverVersion = serverVersion;
        this.serverListPing = serverListPing;
        this.cpuInformation = new LinkedHashMap<>(cpuInformation);
        this.memoryInformation = new LinkedHashMap<>(memoryInformation);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(FIELD_TIMESTAMP, timestamp);
        data.put(FIELD_SERVER_TYPE, serverType);
        data.put(FIELD_SERVER_VERSION, serverVersion);
        data.put(FIELD_SERVER_LIST_PING, serverListPing.toMap());
        data.put(FIELD_CPU_INFORMATION, new LinkedHashMap<>(cpuInformation));
        data.put(FIELD_MEMORY_INFORMATION, new LinkedHashMap<>(memoryInformation));
        return data;
    }
}
