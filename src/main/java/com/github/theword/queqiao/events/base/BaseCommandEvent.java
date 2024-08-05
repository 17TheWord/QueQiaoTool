package com.github.theword.queqiao.events.base;

public class BaseCommandEvent extends BaseMessageEvent {
    public BaseCommandEvent(String eventName, String messageId, BasePlayer player, String command) {
        super( eventName, "player_command", messageId, player, command);
    }
}
