package com.github.theword.queqiao.tool.config;

import lombok.Data;

/**
 * 订阅事件配置
 * <p>默认全部开启</p>
 */
@Data
public class SubscribeEventConfig {

    /**
     * 玩家聊天事件
     */
    private boolean playerChat = true;

    /**
     * 玩家死亡事件
     */
    private boolean playerDeath = true;

    /**
     * 玩家加入事件
     */
    private boolean playerJoin = true;

    /**
     * 玩家退出事件
     */
    private boolean playerQuit = true;

    /**
     * 玩家命令事件
     */
    private boolean playerCommand = true;

    /**
     * 玩家进度事件
     */
    private boolean playerAdvancement = true;
}
