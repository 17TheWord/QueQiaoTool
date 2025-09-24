package com.github.theword.queqiao.tool.config;


import java.util.ArrayList;
import java.util.List;

/**
 * WebSocket Client 配置
 */
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

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public int getReconnectMaxTimes() {
        return reconnectMaxTimes;
    }

    public void setReconnectMaxTimes(int reconnectMaxTimes) {
        this.reconnectMaxTimes = reconnectMaxTimes;
    }

    public List<String> getUrlList() {
        return urlList;
    }

    public void setUrlList(List<String> urlList) {
        this.urlList = urlList;
    }

    public WebSocketClientConfig() {
    }

    public WebSocketClientConfig(boolean enable, int reconnectInterval, int reconnectMaxTimes, List<String> urlList) {
        this.enable = enable;
        this.reconnectInterval = reconnectInterval;
        this.reconnectMaxTimes = reconnectMaxTimes;
        this.urlList = urlList;
    }
}
