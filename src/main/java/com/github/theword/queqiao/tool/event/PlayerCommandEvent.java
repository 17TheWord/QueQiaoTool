package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.player.PlayerMessageEvent;

/**
 * 玩家命令事件
 *
 * @since 0.4.0
 */
public final class PlayerCommandEvent extends PlayerMessageEvent {

    private final String command;

    /**
     * 构造函数
     *
     * @param playerModel 触发事件的玩家
     * @param messageId   消息ID
     * @param rawMessage  原始消息内容
     * @param command     命令内容
     */
    public PlayerCommandEvent(PlayerModel playerModel, String messageId, String rawMessage, String command) {
        super("PlayerCommandEvent", "player_command", playerModel, messageId, rawMessage);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
