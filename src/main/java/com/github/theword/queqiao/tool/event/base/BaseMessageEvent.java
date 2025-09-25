package com.github.theword.queqiao.tool.event.base;

import com.google.gson.annotations.SerializedName;

/**
 * 基础消息事件
 *
 * <p>所有消息事件均继承该类
 *
 * <p>在玩家触发消息类事件时，构建此事件子类并广播
 */
public class BaseMessageEvent extends BaseEvent {

  /** 触发事件的玩家 */
  private final BasePlayer player;

  /** 消息内容 */
  private final String message;

  /** 消息ID，预留字段，目前为空 */
  @SerializedName("message_id")
  private final String messageId;

  /**
   * 构造函数
   *
   * @param eventName 事件名称
   * @param subType 事件子类型
   * @param messageId 消息ID，预留字段，目前为空
   * @param player 发送消息的玩家
   * @param message 消息内容
   */
  public BaseMessageEvent(
      String eventName, String subType, String messageId, BasePlayer player, String message) {
    super(eventName, "message", subType);
    this.messageId = messageId;
    this.player = player;
    this.message = message;
  }
}
