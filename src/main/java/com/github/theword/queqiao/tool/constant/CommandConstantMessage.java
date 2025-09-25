package com.github.theword.queqiao.tool.constant;

/**
 * 命令相关的常量消息
 *
 * <p>包含命令执行过程中用于提示或日志输出的静态字符串。
 */
public class CommandConstantMessage {
    public static final String RELOAD_CONFIG = "加载配置文件完成";

    public static final String RECONNECT_MESSAGE = "正在尝试重连 %s 的 WebSocket 客户端...";
    public static final String RECONNECT_NOT_OPEN_CLIENT = "存在未处于打开状态的 Websocket Client...";
    public static final String RECONNECT_ALL_CLIENT = "正在重连所有 Websocket Client...";
    public static final String RECONNECT_NO_CLIENT_NEED_RECONNECT = "没有客户端需要重连";
    public static final String RECONNECTED = "已重新连接";
}
