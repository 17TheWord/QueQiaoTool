package com.github.theword.queqiao.events.spigot;

import com.github.theword.queqiao.events.base.BasePlayerChatEvent;

public class SpigotAsyncPlayerChatEvent extends BasePlayerChatEvent {

    public SpigotAsyncPlayerChatEvent(SpigotPlayer player, String message) {
        super("AsyncPlayerChatEvent", "", player, message);
    }
}
