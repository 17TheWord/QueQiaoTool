package com.github.theword.queqiao.tool.event;

public class BasePlayerQuitEvent extends BaseNoticeEvent {
    public BasePlayerQuitEvent(String eventName, BasePlayer player) {
        super(eventName, "quit", player);
    }
}
