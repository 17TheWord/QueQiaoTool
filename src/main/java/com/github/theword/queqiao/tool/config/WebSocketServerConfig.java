package com.github.theword.queqiao.tool.config;

import lombok.Data;

@Data
public class WebSocketServerConfig {

    /**
     * 是否启用
     */
    private boolean enable = true;

    /**
     * 服务器地址
     */
    private String host = "127.0.0.1";

    /**
     * 服务器端口
     */
    private int port = 8080;
}
