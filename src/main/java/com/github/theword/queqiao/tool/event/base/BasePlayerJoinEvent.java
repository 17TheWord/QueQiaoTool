package com.github.theword.queqiao.tool.event.base;

/**
 * 通用基础玩家加入事件
 *
 * <p>各服务端需要实现此事件子类
 *
 * <p>在玩家加入服务器时，构造并广播适用于对应服务端的子类事件
 */
public class BasePlayerJoinEvent extends BaseNoticeEvent {

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param player    触发事件的玩家
     */
    public BasePlayerJoinEvent(String eventName, BasePlayer player) {
        super(eventName, "join", player);
    }
}
