package com.github.theword.queqiao.tool.event.base;

public class BasePlayerQuitEvent extends BaseNoticeEvent {
    public BasePlayerQuitEvent(String eventName, BasePlayer player) {
        super( eventName, "quit", player);
    }
}
