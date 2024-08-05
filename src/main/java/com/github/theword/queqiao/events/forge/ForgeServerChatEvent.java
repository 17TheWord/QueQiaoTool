package com.github.theword.queqiao.events.forge;

import com.github.theword.queqiao.events.base.BasePlayerChatEvent;

public final class ForgeServerChatEvent extends BasePlayerChatEvent {
    public ForgeServerChatEvent(String messageId, ForgeServerPlayer player, String message) {
        super("ServerChatEvent", messageId, player, message);
    }

}
