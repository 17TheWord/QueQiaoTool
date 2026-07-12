package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;

public class SendPrivateMessageHandler extends AbstractProtocolHandler<PrivateMessagePayload, PrivateMessageResponse> {
    public SendPrivateMessageHandler() {
        super(PrivateMessagePayload.class);
    }

    @Override
    protected PrivateMessageResponse handlePayload(PrivateMessagePayload payload) throws ProtocolException {
        if ((payload.getNickname() == null || payload.getNickname().isEmpty()) && payload.getUuid() == null) {
            PrivateMessageResponse response = PrivateMessageResponse.playerIsNull();
            throw ProtocolException.badRequest(response.getMessage(), response);
        }
        return GlobalContext.getHandleApiService().handleSendPrivateMessage(payload.getNickname(), payload.getUuid(), payload.getMessage());
    }
}
