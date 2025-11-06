package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.death.DeathModel;
import com.github.theword.queqiao.tool.event.player.PlayerNoticeEvent;

/**
 * 玩家死亡事件
 *
 * @since 0.4.0
 */
public final class PlayerDeathEvent extends PlayerNoticeEvent {

    private final DeathModel death;

    /**
     * 构造函数
     *
     * @param playerModel 触发事件的玩家
     */
    public PlayerDeathEvent(PlayerModel playerModel, DeathModel deathModel) {
        super("PlayerDeathEvent", "player_death", playerModel);
        this.death = deathModel;
    }

    public DeathModel getDeath() {
        return death;
    }

}
