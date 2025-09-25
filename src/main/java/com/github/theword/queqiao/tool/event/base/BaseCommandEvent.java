package com.github.theword.queqiao.tool.event.base;

/**
 * 通用基础命令事件
 *
 * <p>各服务端需要实现此事件子类
 *
 * <p>在玩家触发命令事件时，构造并广播适用于对应服务端的子类事件
 */
public class BaseCommandEvent extends BaseMessageEvent {

  /**
   * 构造函数
   *
   * @param eventName 事件名称
   * @param messageId 消息ID，预留字段，目前为空
   * @param player 发送消息的玩家
   * @param command 命令内容
   */
  public BaseCommandEvent(String eventName, String messageId, BasePlayer player, String command) {
    super(eventName, "player_command", messageId, player, command);
  }
}
