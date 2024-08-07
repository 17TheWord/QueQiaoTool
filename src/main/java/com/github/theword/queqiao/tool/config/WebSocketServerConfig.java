package com.github.theword.queqiao.tool.config;

import lombok.Data;

@Data
public class WebSocketServerConfig {
    private boolean enable = false;
    private String host = "127.0.0.1";
    private int port = 8080;
}
