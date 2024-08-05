package com.github.theword.queqiao.events.base;

public class BasePlayerJoinEvent extends BaseNoticeEvent {
    public BasePlayerJoinEvent(String eventName, BasePlayer player) {
        super( eventName, "join", player);
    }
}
