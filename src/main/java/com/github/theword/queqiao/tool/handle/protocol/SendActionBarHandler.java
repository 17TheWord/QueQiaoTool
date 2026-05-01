package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;

/**
 * 处理 ActionBar 消息 API。
 */
public class SendActionBarHandler implements ProtocolHandler {
    private final Gson gson;
    private final HandleApiService handleApiService;

    public SendActionBarHandler(Gson gson, HandleApiService handleApiService) {
        this.gson = gson;
        this.handleApiService = handleApiService;
    }

    @Override
    public Response handle(BasePayload payload) {
        MessagePayload actionMessagePayload = gson.fromJson(payload.getData(), MessagePayload.class);
        handleApiService.handleSendActionBarMessage(actionMessagePayload.getMessage());
        return Response.success();
    }
}
