package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;

/**
 * 处理私聊消息 API。
 */
public class SendPrivateMessageHandler implements ProtocolHandler {
    private final Gson gson;
    private final HandleApiService handleApiService;

    public SendPrivateMessageHandler(Gson gson, HandleApiService handleApiService) {
        this.gson = gson;
        this.handleApiService = handleApiService;
    }

    @Override
    public Response handle(BasePayload payload) {
        PrivateMessagePayload privateMessagePayload = gson.fromJson(payload.getData(), PrivateMessagePayload.class);
        if ((privateMessagePayload.getNickname() == null || privateMessagePayload.getNickname().isEmpty()) && privateMessagePayload.getUuid() == null) {
            return Response.failed(400, PrivateMessageResponse.playerIsNull().getMessage(), PrivateMessageResponse.playerIsNull());
        }
        PrivateMessageResponse privateMessageResponse = handleApiService.handleSendPrivateMessage(privateMessagePayload.getNickname(), privateMessagePayload.getUuid(), privateMessagePayload.getMessage());
        return Response.success(privateMessageResponse);
    }
}
