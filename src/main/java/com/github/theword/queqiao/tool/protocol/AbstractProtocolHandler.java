package com.github.theword.queqiao.tool.protocol;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;

public abstract class AbstractProtocolHandler<P, R> {
    private final Class<P> payloadType;

    protected AbstractProtocolHandler(Class<P> payloadType) {
        this.payloadType = payloadType;
    }

    public final R handle(JsonElement data) throws ProtocolException {
        if (payloadType == EmptyPayload.class) {
            return handlePayload(payloadType.cast(EmptyPayload.INSTANCE));
        }
        P payload;
        try {
            payload = GsonUtils.getGson().fromJson(data, payloadType);
        } catch (JsonParseException | IllegalStateException e) {
            throw ProtocolException.badRequest(ProtocolConstants.Message.PARSE_DATA_FAILED, data);
        }
        if (payload == null) {
            throw ProtocolException.badRequest(ProtocolConstants.Message.PARSE_DATA_FAILED, data);
        }
        return handlePayload(payload);
    }

    protected abstract R handlePayload(P payload) throws ProtocolException;
}
