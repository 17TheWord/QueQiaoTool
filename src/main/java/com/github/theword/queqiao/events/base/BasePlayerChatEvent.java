package com.github.theword.queqiao.events.base;

public class BasePlayerChatEvent extends BaseMessageEvent {
    public BasePlayerChatEvent(String eventName, String messageId, BasePlayer player, String message) {
        super(eventName, "chat", messageId, player, message);
    }

}
