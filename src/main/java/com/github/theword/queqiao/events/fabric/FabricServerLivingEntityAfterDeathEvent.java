package com.github.theword.queqiao.events.fabric;

import com.github.theword.queqiao.events.base.BasePlayerDeathEvent;

public class FabricServerLivingEntityAfterDeathEvent extends BasePlayerDeathEvent {

    public FabricServerLivingEntityAfterDeathEvent(String messageId, FabricServerPlayer player, String message) {
        super("ServerLivingEntityAfterDeathEvent", messageId, player, message);
    }
}
