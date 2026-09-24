package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import org.slf4j.Logger;

public class SendCommandHandler extends AbstractProtocolHandler<EmptyPayload, Void> {

    public SendCommandHandler(Logger logger, HandleApiService handleApiService) {
        super(logger, handleApiService, EmptyPayload.class);
    }

    @Override
    protected Void handlePayload(EmptyPayload payload) throws ProtocolException {
        throw ProtocolException.internalError(ProtocolConstants.Message.SEND_COMMAND_UNSUPPORTED);
    }
}
