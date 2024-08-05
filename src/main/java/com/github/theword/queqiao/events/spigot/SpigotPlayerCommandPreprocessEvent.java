package com.github.theword.queqiao.events.spigot;

import com.github.theword.queqiao.events.base.BaseCommandEvent;

public class SpigotPlayerCommandPreprocessEvent extends BaseCommandEvent {

    public SpigotPlayerCommandPreprocessEvent( SpigotPlayer player, String command) {
        super("PlayerCommandPreprocessEvent", "", player, command);
    }
}
