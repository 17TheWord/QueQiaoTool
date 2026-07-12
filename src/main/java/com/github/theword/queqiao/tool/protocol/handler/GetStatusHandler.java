package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.protocol.handler.status.ServerStatusCollector;

import java.util.Map;

public class GetStatusHandler extends AbstractProtocolHandler<EmptyPayload, Map<String, Object>> {
    public GetStatusHandler() {
        super(EmptyPayload.class);
    }

    @Override
    protected Map<String, Object> handlePayload(EmptyPayload payload) {
        GlobalContext.getLogger().info("收到 get_status 请求，已返回服务器状态");
        return ServerStatusCollector.collectStatusSnapshot();
    }
}
