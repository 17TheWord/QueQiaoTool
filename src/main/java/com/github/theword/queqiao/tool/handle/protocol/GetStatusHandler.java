package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.github.theword.queqiao.tool.utils.ServerStatusCollector;
import org.slf4j.Logger;

/**
 * 处理服务器状态查询 API。
 */
public class GetStatusHandler implements ProtocolHandler {
    private final Logger logger;

    public GetStatusHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Response handle(BasePayload payload) {
        logger.info("收到 get_status 请求，已返回服务器状态");
        return Response.success(ServerStatusCollector.collectStatusSnapshot());
    }
}
