package com.github.theword.queqiao.events.forge;

import com.github.theword.queqiao.events.base.BasePlayerDeathEvent;

public class ForgePlayerDeathEvent extends BasePlayerDeathEvent {

    public ForgePlayerDeathEvent(String messageId, ForgeServerPlayer player, String message) {
        super("PlayerDeathEvent", messageId, player, message);
    }
}
