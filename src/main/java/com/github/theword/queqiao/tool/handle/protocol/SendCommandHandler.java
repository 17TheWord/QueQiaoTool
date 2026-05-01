package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.constant.ApiConstants;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.response.Response;

/**
 * 处理命令发送 API。
 */
public class SendCommandHandler implements ProtocolHandler {
    @Override
    public Response handle(BasePayload payload) {
        return Response.failed(ApiConstants.Code.INTERNAL_ERROR, String.format(ApiConstants.Message.SEND_COMMAND_UNSUPPORTED, payload.getApi()));
    }
}
