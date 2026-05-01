package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;

/**
 * 处理广播消息 API。
 */
public class BroadcastHandler implements ProtocolHandler {
    private final Gson gson;
    private final HandleApiService handleApiService;

    public BroadcastHandler(Gson gson, HandleApiService handleApiService) {
        this.gson = gson;
        this.handleApiService = handleApiService;
    }

    @Override
    public Response handle(BasePayload payload) {
        MessagePayload messagePayload = gson.fromJson(payload.getData(), MessagePayload.class);
        handleApiService.handleBroadcastMessage(messagePayload.getMessage());
        return Response.success();
    }
}
