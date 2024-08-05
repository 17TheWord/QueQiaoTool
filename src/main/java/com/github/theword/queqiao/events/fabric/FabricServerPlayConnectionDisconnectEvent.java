package com.github.theword.queqiao.events.fabric;

import com.github.theword.queqiao.events.base.BasePlayerQuitEvent;

public class FabricServerPlayConnectionDisconnectEvent extends BasePlayerQuitEvent {
    public FabricServerPlayConnectionDisconnectEvent(FabricServerPlayer player) {
        super("ServerPlayConnectionDisconnectEvent", player);
    }
}
