package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;

public class SendCommandHandler extends AbstractProtocolHandler<EmptyPayload, Void> {
    public SendCommandHandler() {
        super(EmptyPayload.class);
    }

    @Override
    protected Void handlePayload(EmptyPayload payload) throws ProtocolException {
        throw ProtocolException.internalError(ProtocolConstants.Message.SEND_COMMAND_UNSUPPORTED);
    }
}
