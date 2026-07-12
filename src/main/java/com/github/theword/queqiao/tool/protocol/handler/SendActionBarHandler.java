package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;

public class SendActionBarHandler extends AbstractProtocolHandler<MessagePayload, Void> {
    public SendActionBarHandler() {
        super(MessagePayload.class);
    }

    @Override
    protected Void handlePayload(MessagePayload payload) {
        GlobalContext.getHandleApiService().handleSendActionBarMessage(payload.getMessage());
        return null;
    }
}
