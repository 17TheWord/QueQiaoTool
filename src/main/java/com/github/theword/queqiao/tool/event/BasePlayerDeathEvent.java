package com.github.theword.queqiao.tool.event;

public class BasePlayerDeathEvent extends BaseMessageEvent {

    public BasePlayerDeathEvent(String eventName, String messageId, BasePlayer player, String message) {
        super(eventName, "death", messageId, player, message);
    }
}
