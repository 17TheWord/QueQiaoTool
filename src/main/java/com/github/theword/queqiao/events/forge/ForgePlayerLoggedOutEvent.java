package com.github.theword.queqiao.events.forge;

import com.github.theword.queqiao.events.base.BasePlayerQuitEvent;

public final class ForgePlayerLoggedOutEvent extends BasePlayerQuitEvent {
    public ForgePlayerLoggedOutEvent(ForgeServerPlayer player) {
        super("PlayerLoggedOutEvent", player);
    }
}
