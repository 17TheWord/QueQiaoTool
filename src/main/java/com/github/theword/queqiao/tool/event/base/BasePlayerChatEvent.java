package com.github.theword.queqiao.tool.event.base;

/**
 * 通用基础玩家聊天事件
 *
 * <p>各服务端需要实现此事件子类
 *
 * <p>在玩家触发聊天事件时，构造并广播适用于对应服务端的子类事件
 */
public class BasePlayerChatEvent extends BaseMessageEvent {

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param messageId 消息ID，预留字段，目前为空
     * @param player    发送消息的玩家
     * @param message   消息内容
     */
    public BasePlayerChatEvent(
                               String eventName, String messageId, BasePlayer player, String message) {
        super(eventName, "chat", messageId, player, message);
    }
}
