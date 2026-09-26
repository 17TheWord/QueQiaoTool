package io.github.theword.queqiao.core.event.player;

import io.github.theword.queqiao.core.event.base.BaseMessageEvent;
import io.github.theword.queqiao.core.event.model.PlayerModel;

public class PlayerMessageEvent extends BaseMessageEvent {

    private final PlayerModel player;

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param subType   事件子类型
     */
    public PlayerMessageEvent(String eventName, String subType, PlayerModel playerModel, String messageId, String rawMessage) {
        super(eventName, subType, messageId, rawMessage);
        this.player = playerModel;
    }

    public PlayerModel getPlayer() {
        return player;
    }
}
