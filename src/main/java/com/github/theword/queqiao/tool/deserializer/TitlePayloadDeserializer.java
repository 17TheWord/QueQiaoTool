package com.github.theword.queqiao.tool.deserializer;

import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.utils.PayloadUtils;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * TitlePayload 反序列化器
 * <p> 用于将 JSON 数据反序列化为 TitlePayload 对象 </p>
 */
public class TitlePayloadDeserializer implements JsonDeserializer<TitlePayload> {

    /**
     * 反序列化 Title 消息
     * <p>接管所有 TitlePayload 及其子类的反序列化任务</p>
     *
     * @param json    Json数据
     * @param type    目标类型
     * @param context Json反序列化上下文
     * @return TitlePayload
     * @throws JsonParseException Json反序列化异常
     */
    @Override
    public TitlePayload deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        TitlePayload payload = new TitlePayload();
        JsonObject jsonObject = json.getAsJsonObject();

        // Deserialize title
        JsonElement titleElement = jsonObject.get("title");
        if (titleElement != null) {
            payload.setTitle(PayloadUtils.deserializeMessageSegmentList(titleElement, context));
        }

        // Deserialize subtitle
        JsonElement subtitleElement = jsonObject.get("subtitle");
        if (subtitleElement != null) {
            payload.setSubtitle(PayloadUtils.deserializeMessageSegmentList(subtitleElement, context));
        }

        // Deserialize other fields
        if (jsonObject.has("fadein")) {
            payload.setFadein(jsonObject.get("fadein").getAsInt());
        }
        if (jsonObject.has("stay")) {
            payload.setStay(jsonObject.get("stay").getAsInt());
        }
        if (jsonObject.has("fadeout")) {
            payload.setFadeout(jsonObject.get("fadeout").getAsInt());
        }

        return payload;
    }
}
