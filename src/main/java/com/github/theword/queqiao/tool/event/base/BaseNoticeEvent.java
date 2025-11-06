package com.github.theword.queqiao.tool.event.base;

/**
 * BaseNoticeEvent
 *
 * <p>基础通知类事件
 *
 * <p>所有通知类事件的基类
 */
public class BaseNoticeEvent extends BaseEvent {
    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param subType   事件子类型
     */
    public BaseNoticeEvent(String eventName, String subType) {
        super(eventName, "notice", subType);
    }
}
