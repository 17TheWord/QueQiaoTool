package com.github.theword.queqiao.events.base;

public class BasePlayerDeathEvent extends BaseMessageEvent {

    public BasePlayerDeathEvent(String eventName, String messageId, BasePlayer player, String message) {
        super( eventName, "death", messageId, player, message);
    }
}
