package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import org.slf4j.Logger;

public class BroadcastHandler extends AbstractProtocolHandler<MessagePayload, Void> {
    public BroadcastHandler(Logger logger, HandleApiService handleApiService) {
        super(logger, handleApiService, MessagePayload.class);
    }

    @Override
    protected Void handlePayload(MessagePayload payload) {
        this.handleApiService.handleBroadcastMessage(payload.getMessage());
        return null;
    }
}
