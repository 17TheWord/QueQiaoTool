package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import org.slf4j.Logger;

public class SendActionBarHandler extends AbstractProtocolHandler<MessagePayload, Void> {
    public SendActionBarHandler(Logger logger) {
        super(logger, MessagePayload.class);
    }

    @Override
    protected Void handlePayload(MessagePayload payload) {
        GlobalContext.getHandleApiService().handleSendActionBarMessage(payload.getMessage());
        return null;
    }
}
