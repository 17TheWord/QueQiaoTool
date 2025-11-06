package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.player.PlayerNoticeEvent;

/**
 * 玩家离开事件
 *
 * @since 0.4.0
 */
public final class PlayerQuitEvent extends PlayerNoticeEvent {

    /**
     * 构造函数
     *
     * @param playerModel 触发事件的玩家
     */
    public PlayerQuitEvent(PlayerModel playerModel) {
        super("PlayerQuitEvent", "player_quit", playerModel);
    }
}
