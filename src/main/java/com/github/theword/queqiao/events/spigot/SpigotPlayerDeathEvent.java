package com.github.theword.queqiao.events.spigot;

import com.github.theword.queqiao.events.base.BasePlayerDeathEvent;

public class SpigotPlayerDeathEvent extends BasePlayerDeathEvent {

    public SpigotPlayerDeathEvent( SpigotPlayer player, String message) {
        super( "PlayerDeathEvent", "", player, message);
    }
}
