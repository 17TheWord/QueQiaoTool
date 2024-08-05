package com.github.theword.queqiao.events.fabric;

import com.github.theword.queqiao.events.base.BasePlayerJoinEvent;

public class FabricServerPlayConnectionJoinEvent extends BasePlayerJoinEvent {
    public FabricServerPlayConnectionJoinEvent(FabricServerPlayer player) {
        super("ServerPlayConnectionJoinEvent", player);
    }
}
