package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.response.Response;

/**
 * 单个协议 API 的处理器。
 */
public interface ProtocolHandler {
    /**
     * 处理协议请求。
     *
     * @param payload 基础请求负载
     * @return 响应
     */
    Response handle(BasePayload payload);
}
