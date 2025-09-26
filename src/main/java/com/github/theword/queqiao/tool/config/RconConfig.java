package com.github.theword.queqiao.tool.config;

/** Rcon 客户端配置 */
public class RconConfig {
    /** 是否启用 Rcon 客户端 */
    private boolean enable = false;
    /** Rcon 端口 */
    private int port = 25575;
    /** Rcon 密码 */
    private String password = "";

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
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

}
