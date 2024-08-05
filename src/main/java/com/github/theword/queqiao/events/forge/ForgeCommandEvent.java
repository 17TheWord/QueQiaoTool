package com.github.theword.queqiao.events.forge;

import com.github.theword.queqiao.events.base.BaseCommandEvent;

public class ForgeCommandEvent extends BaseCommandEvent {
    public ForgeCommandEvent(String messageId, ForgeServerPlayer player, String command) {
        super("CommandEvent", messageId, player, command);
    }
}
