package com.github.theword.queqiao.events.base;

import com.google.gson.annotations.SerializedName;

public class BaseMessageEvent extends BaseEvent {
    @SerializedName("message_id")
    private String messageId;
    private final BasePlayer player;
    private final String message;

    public BaseMessageEvent(String eventName, String subType, String messageId, BasePlayer player, String message) {
        super(eventName, "message", subType);
        this.messageId = messageId;
        this.player = player;
        this.message = message;
    }
}
