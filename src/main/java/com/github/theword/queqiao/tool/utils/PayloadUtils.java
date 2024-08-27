package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.List;

public class PayloadUtils {

    public static List<MessageSegment> deserializeMessageSegmentList(JsonElement element, JsonDeserializationContext context) {
        if (element.isJsonArray()) {
            return context.deserialize(element, new TypeToken<List<MessageSegment>>() {}.getType());
        } else if (element.isJsonPrimitive()) {
            String text = element.getAsString();
            MessageSegment segment = new MessageSegment("text", new CommonTextComponent(text));
            return Collections.singletonList(segment);
        } else {
            MessageSegment messageSegment = new MessageSegment("text", new CommonTextComponent("Unknown Message"));
            return Collections.singletonList(messageSegment);
        }
    }
}