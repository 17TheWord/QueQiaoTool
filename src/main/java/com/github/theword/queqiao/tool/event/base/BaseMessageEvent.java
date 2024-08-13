package com.github.theword.queqiao.tool.event.base;

import com.google.gson.annotations.SerializedName;

public class BaseMessageEvent extends BaseEvent {
    private final BasePlayer player;
    private final String message;
    @SerializedName("message_id")
    private final String messageId;

    public BaseMessageEvent(String eventName, String subType, String messageId, BasePlayer player, String message) {
        super(eventName, "message", subType);
        this.messageId = messageId;
        this.player = player;
        this.message = message;
    }
}
