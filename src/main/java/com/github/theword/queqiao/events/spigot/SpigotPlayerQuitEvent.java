package com.github.theword.queqiao.events.spigot;

import com.github.theword.queqiao.events.base.BasePlayerQuitEvent;

public class SpigotPlayerQuitEvent extends BasePlayerQuitEvent {

    public SpigotPlayerQuitEvent( SpigotPlayer player) {
        super("PlayerQuitEvent", player);
    }
}
