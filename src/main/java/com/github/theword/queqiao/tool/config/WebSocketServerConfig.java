package com.github.theword.queqiao.tool.config;

public class WebSocketServerConfig {

    /** 是否启用 WebSocket 服务端 */
    private boolean enable = true;

    /** 监听地址 */
    private String host = "127.0.0.1";

    /** 监听端口 */
    private int port = 8080;

    /** 是否将非 WebSocket 流量转发到 Minecraft 服务器 */
    private boolean forward = false;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isForward() {
        return forward;
    }

    public void setForward(boolean forward) {
        this.forward = forward;
    }

    public WebSocketServerConfig() {
    }

    public WebSocketServerConfig(boolean enable, String host, int port) {
        this(enable, host, port, false);
    }

    public WebSocketServerConfig(boolean enable, String host, int port, boolean forward) {
        this.enable = enable;
        this.host = host;
        this.port = port;
        this.forward = forward;
    }
}
