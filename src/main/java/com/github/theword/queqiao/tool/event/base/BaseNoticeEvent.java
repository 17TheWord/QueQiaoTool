package com.github.theword.queqiao.tool.event.base;

/**
 * 基础通知事件
 *
 * <p>所有通知事件均继承该类
 *
 * <p>在玩家触发通知类事件时，构建此事件子类并广播
 */
public class BaseNoticeEvent extends BaseEvent {

  /** 触发事件的玩家 */
  private final BasePlayer player;

  /**
   * 构造函数
   *
   * @param eventName 事件名称
   * @param subType 事件子类型
   * @param player 触发事件的玩家
   */
  public BaseNoticeEvent(String eventName, String subType, BasePlayer player) {
    super(eventName, "notice", subType);
    this.player = player;
  }
}
