package com.github.theword.queqiao.tool.event.base;

public class BaseAchievementEvent extends BaseNoticeEvent{
    public BaseAchievementEvent(String eventName, String subType, BasePlayer player) {
        super(eventName, "achievement", player);
    }
}
