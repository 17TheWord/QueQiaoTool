package com.github.theword.queqiao.tool.event;

public class BasePlayerJoinEvent extends BaseNoticeEvent {
    public BasePlayerJoinEvent(String eventName, BasePlayer player) {
        super(eventName, "join", player);
    }
}
