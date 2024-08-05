package com.github.theword.queqiao.events.spigot;

import com.github.theword.queqiao.events.base.BasePlayerJoinEvent;

public class SpigotPlayerJoinEvent extends BasePlayerJoinEvent {

    public SpigotPlayerJoinEvent( SpigotPlayer player) {
        super( "PlayerJoinEvent", player);
    }

}
