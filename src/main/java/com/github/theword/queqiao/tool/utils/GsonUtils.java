package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.deserializer.MessagePayloadDeserializer;
import com.github.theword.queqiao.tool.deserializer.TitlePayloadDeserializer;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {

    public static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(MessagePayload.class, new MessagePayloadDeserializer())
                .registerTypeAdapter(TitlePayload.class, new TitlePayloadDeserializer())
                .registerTypeAdapter(PrivateMessagePayload.class, new MessagePayloadDeserializer())
                .create();
    }

}
