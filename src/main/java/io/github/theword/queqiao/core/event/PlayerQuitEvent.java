package io.github.theword.queqiao.core.event;

import io.github.theword.queqiao.core.event.model.PlayerModel;
import io.github.theword.queqiao.core.event.player.PlayerNoticeEvent;

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
