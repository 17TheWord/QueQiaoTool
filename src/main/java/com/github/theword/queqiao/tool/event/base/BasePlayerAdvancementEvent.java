package com.github.theword.queqiao.tool.event.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BasePlayerAdvancementEvent extends BaseNoticeEvent {
    private final BaseAdvancement advancement;

    public BasePlayerAdvancementEvent(String eventName, BasePlayer player, BaseAdvancement advancement) {
        super(eventName, "achievement", player);
        this.advancement = advancement;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseAdvancement {
        /**
         * Common field
         */
        private String text;
    }
}
