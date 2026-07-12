package com.github.theword.queqiao.tool.protocol.handler.status;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MinecraftPingResponse {
    private final Map<String, Object> rawData;

    private MinecraftPingResponse(Map<String, Object> rawData) {
        this.rawData = new LinkedHashMap<>(rawData);
    }

    public static MinecraftPingResponse fromRawData(Map<String, Object> rawData) {
        return new MinecraftPingResponse(rawData);
    }

    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(rawData);
    }
}
