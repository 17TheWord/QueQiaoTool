package com.github.theword.queqiao.events.fabric;

import com.github.theword.queqiao.events.base.BasePlayerChatEvent;

public class FabricServerMessageEvent extends BasePlayerChatEvent {
    public FabricServerMessageEvent(String messageId, FabricServerPlayer player, String message) {
        super("ServerMessageEvent", messageId, player, message);
    }
}
