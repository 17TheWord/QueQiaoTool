package com.github.theword.queqiao.tool.event.base;

public class BaseCommandEvent extends BaseMessageEvent {
    public BaseCommandEvent(String eventName, String messageId, BasePlayer player, String command) {
        super(eventName, "player_command", messageId, player, command);
    }
}
