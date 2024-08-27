package com.github.theword.queqiao.tool.deserializer;

import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.utils.PayloadUtils;
import com.google.gson.*;

import java.lang.reflect.Type;

public class TitlePayloadDeserializer implements JsonDeserializer<TitlePayload> {
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
