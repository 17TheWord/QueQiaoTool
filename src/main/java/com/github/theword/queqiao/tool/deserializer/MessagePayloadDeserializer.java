package com.github.theword.queqiao.tool.deserializer;

import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload;
import com.github.theword.queqiao.tool.utils.PayloadUtils;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.UUID;

public class MessagePayloadDeserializer implements JsonDeserializer<MessagePayload> {

    /**
     * 反序列化Message消息
     * <p>接管所有 MessagePayload 及其子类的反序列化任务</p>
     *
     * @param json    Json数据
     * @param typeOfT 目标类型
     * @param context Json反序列化上下文
     * @return ? extends MessagePayload
     * @throws JsonParseException Json反序列化异常
     */
    @Override
    public MessagePayload deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (jsonObject.has("uuid") || jsonObject.has("nickname")) {
            PrivateMessagePayload privatePayload = new PrivateMessagePayload();
            privatePayload.setUuid(context.deserialize(jsonObject.get("uuid"), UUID.class));
            privatePayload.setNickname(context.deserialize(jsonObject.get("nickname"), String.class));
            privatePayload.setMessage(PayloadUtils.deserializeMessageSegmentList(jsonObject.get("message"), context));
            return privatePayload;
        } else {
            MessagePayload payload = new MessagePayload();
            JsonElement messageElement = jsonObject.get("message");
            payload.setMessage(PayloadUtils.deserializeMessageSegmentList(messageElement, context));
            return payload;
        }
    }
}
