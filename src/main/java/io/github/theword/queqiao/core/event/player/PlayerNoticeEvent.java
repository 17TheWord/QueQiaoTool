package io.github.theword.queqiao.core.event.player;

import io.github.theword.queqiao.core.event.base.BaseNoticeEvent;
import io.github.theword.queqiao.core.event.model.PlayerModel;

public class PlayerNoticeEvent extends BaseNoticeEvent {

    private final PlayerModel player;

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param subType   事件子类型
     */
    public PlayerNoticeEvent(String eventName, String subType, PlayerModel playerModel) {
        super(eventName, subType);
        this.player = playerModel;
    }

    public PlayerModel getPlayer() {
        return player;
    }
}
