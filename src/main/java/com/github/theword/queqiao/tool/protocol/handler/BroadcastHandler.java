package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;

public class BroadcastHandler extends AbstractProtocolHandler<MessagePayload, Void> {
    public BroadcastHandler() {
        super(MessagePayload.class);
    }

    @Override
    protected Void handlePayload(MessagePayload payload) {
        GlobalContext.getHandleApiService().handleBroadcastMessage(payload.getMessage());
        return null;
    }
}
