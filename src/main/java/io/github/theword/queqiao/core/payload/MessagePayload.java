package io.github.theword.queqiao.core.payload;

import com.google.gson.JsonElement;


/**
 * 消息负载类
 */
public class MessagePayload {

    private JsonElement message;

    public MessagePayload(JsonElement message) {
        this.message = message;
    }

    public MessagePayload() {
    }

    public JsonElement getMessage() {
        return message;
    }

    public void setMessage(JsonElement message) {
        this.message = message;
    }
}
