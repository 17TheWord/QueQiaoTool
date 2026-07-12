package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;

public class SendTitleHandler extends AbstractProtocolHandler<TitlePayload, Void> {
    public SendTitleHandler() {
        super(TitlePayload.class);
    }

    @Override
    protected Void handlePayload(TitlePayload payload) throws ProtocolException {
        if ((payload.getTitle() == null || payload.getTitle().isJsonNull()) && (payload.getSubtitle() == null || payload.getSubtitle().isJsonNull())) {
            throw ProtocolException.badRequest(ProtocolConstants.Message.TITLE_AND_SUBTITLE_EMPTY);
        }
        GlobalContext.getHandleApiService().handleSendTitleMessage(payload.getTitle(), payload.getSubtitle(), payload.getFadeIn(), payload.getStay(), payload.getFadeOut());
        return null;
    }
}
