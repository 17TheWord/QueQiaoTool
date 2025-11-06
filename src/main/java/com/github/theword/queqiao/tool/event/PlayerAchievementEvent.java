package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.achievement.AchievementModel;
import com.github.theword.queqiao.tool.event.player.PlayerNoticeEvent;

/**
 * 玩家成就事件
 *
 * @since 0.4.0
 */
public final class PlayerAchievementEvent extends PlayerNoticeEvent {

    private final AchievementModel achievement;

    /**
     * 构造函数
     *
     * @param playerModel      触发事件的玩家
     * @param achievementModel 成就对象
     */
    public PlayerAchievementEvent(PlayerModel playerModel, AchievementModel achievementModel) {
        super("PlayerAchievementEvent", "player_achievement", playerModel);
        this.achievement = achievementModel;
    }

    public AchievementModel getAchievement() {
        return achievement;
    }

}
