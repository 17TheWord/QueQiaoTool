package com.github.theword.queqiao.tool.config;

/** Rcon 客户端配置 */
public class RconConfig {
    /** 是否启用 Rcon 客户端 */
    private boolean enable = false;
    /** Rcon 服务器地址 */
    private String host = "127.0.0.1";
    /** Rcon 端口 */
    private int port = 25575;
    /** Rcon 密码 */
    private String password = "";
    /** 最大重连次数 */
    private int reconnectMaxTimes = 5;
    /** 重连间隔（秒） */
    private int reconnectInterval = 5;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getReconnectMaxTimes() {
        return reconnectMaxTimes;
    }

    public void setReconnectMaxTimes(int reconnectMaxTimes) {
        this.reconnectMaxTimes = reconnectMaxTimes;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }
}
