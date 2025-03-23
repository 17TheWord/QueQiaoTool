package com.github.theword.queqiao.tool.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket Client 配置
 */
@Data
public class WebSocketClientConfig {

    /**
     * 是否启用 WebSocket Client
     */
    private boolean enable = false;

    /**
     * 重连间隔
     * 单位：秒
     */
    private int reconnectInterval = 5;
    /**
     * 最大重连次数
     * 单位：次
     */
    private int reconnectMaxTimes = 5;
    /**
     * WebSocket URL 列表
     */
    private List<String> urlList = new ArrayList<>();
}
