package io.github.theword.queqiao.core.protocol.handler;

import io.github.theword.queqiao.core.handle.HandleApiService;
import io.github.theword.queqiao.core.payload.MessagePayload;
import io.github.theword.queqiao.core.protocol.AbstractProtocolHandler;
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
