package com.github.theword.queqiao.constant;

public class CommandConstantMessage {
    public static final String RELOAD_CONFIG = "配置文件重载完成";

    public static final String RECONNECT_MESSAGE = "正在尝试重连至：%s 的 WebSocket 服务器...";
    public static final String RECONNECT_NOT_OPEN_CLIENT = "即将重新连接未处于开启状态的 Websocket Client...";
    public static final String RECONNECT_ALL_CLIENT = "即将重新连接所有的 Websocket Client...";
    public static final String RECONNECT_NO_CLIENT_NEED_RECONNECT = "没有需要重连的 Websocket Client";
    public static final String RECONNECTED = "重连任务已完成";
}
