package com.github.theword.queqiao.configs;

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
    private boolean enable = true;
    /**
     * 重连间隔
     */
    private int reconnect_interval = 5;
    /**
     * 最大重连次数
     */
    private int reconnect_max_times = 5;
    /**
     * WebSocket URL 列表
     */
    private List<String> url_list = new ArrayList<>();
}
