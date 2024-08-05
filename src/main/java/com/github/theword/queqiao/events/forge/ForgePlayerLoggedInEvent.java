package com.github.theword.queqiao.events.forge;

import com.github.theword.queqiao.events.base.BasePlayerJoinEvent;

public final class ForgePlayerLoggedInEvent extends BasePlayerJoinEvent {
    public ForgePlayerLoggedInEvent(ForgeServerPlayer player) {
        super("PlayerLoggedInEvent", player);
    }
}
