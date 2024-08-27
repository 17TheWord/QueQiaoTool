package com.github.theword.queqiao.tool.deserializer;

import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.utils.PayloadUtils;
import com.google.gson.*;

import java.lang.reflect.Type;

public class MessagePayloadDeserializer implements JsonDeserializer<MessagePayload> {

    @Override
    public MessagePayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        MessagePayload payload = new MessagePayload();

        JsonElement messageElement = json.getAsJsonObject().get("message");
        payload.setMessage(PayloadUtils.deserializeMessageSegmentList(messageElement, context));

        return payload;
    }
}
