package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.player.PlayerMessageEvent;

/**
 * 玩家聊天事件
 *
 * @since 0.4.0
 */
public final class PlayerChatEvent extends PlayerMessageEvent {


    private final String message;

    /**
     * 构造函数
     *
     * @param playerModel 触发事件的玩家
     * @param messageId   消息ID
     * @param rawMessage  原始消息内容
     * @param message     消息内容
     */
    public PlayerChatEvent(PlayerModel playerModel, String messageId, String rawMessage, String message) {
        super("PlayerChatEvent", "player_chat", playerModel, messageId, rawMessage);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
