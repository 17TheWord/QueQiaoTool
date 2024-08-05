package com.github.theword.queqiao.events.fabric;

import com.github.theword.queqiao.events.base.BaseCommandEvent;

public class FabricServerCommandMessageEvent extends BaseCommandEvent {
    public FabricServerCommandMessageEvent(String messageId, FabricServerPlayer player, String message) {
        super("ServerCommandMessageEvent", messageId, player, message);
    }
}
