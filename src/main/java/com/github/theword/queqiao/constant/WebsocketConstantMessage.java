package com.github.theword.queqiao.constant;

public class WebsocketConstantMessage {
    public static final String PARSE_MESSAGE_ERROR_ON_MESSAGE = "解析来自 %s 的 WebSocket 消息时出现异常";
    public static final String CLOSE_BY_RELOAD = "Websocket 正在重载";


    public static class Client {
        private static final String CONNECT_TO = "连接至：%s 的 ";

        public static final String SEND_MESSAGE = "发送至 %s 的消息：%s";

        public static final String LAUNCHING = "WebSocket Client 正在启动...";

        public static final String CLOSING_CONNECTION = CONNECT_TO + "WebSocket Client 正在关闭。";

        public static final String RECONNECTING = CONNECT_TO + "WebSocket 连接已断开，尝试第 %s 次重连...";

        public static final String CONNECT_SUCCESSFUL = "已成功连接至 %s 的 WebSocket 服务器！";

        public static final String CONNECTION_ERROR = CONNECT_TO + "WebSocket 连接出现异常：%s";

        public static final String CONNECTION_NOT_OPEN = CONNECT_TO + "WebSocket 连接未打开";

        public static final String SEND_MESSAGE_FAILED = CONNECTION_NOT_OPEN + "，发送消息 %s 失败";

        public static final String URI_SYNTAX_ERROR = CONNECT_TO + "WebSocket URL 格式错误，无法连接！";

        public static final String MAX_RECONNECT_ATTEMPTS_REACHED = CONNECT_TO + "重连次数达到最大值，将不再自动重连，请使用命令手动重连！";

        public static final String CLEAR_WEBSOCKET_CLIENT_LIST = "已清空 Websocket Client 列表。";

        public static final String RELOADING = "Websocket Client 正在重载";
        public static final String RELOADED = "Websocket Client 重载完毕";
    }

    public static class Server {
        public static final String ERROR_ON_STOPPING = "Websocket Server 正在启动时出现异常。";

        public static final String BROADCAST_MESSAGE = "向所有客户端广播消息：%s";
        public static final String RELOADING = "Websocket Server 正在重载";
        public static final String RELOADED = "Websocket Server 重载完毕";

        private static final String CLIENT_PREFIX = "来自：%s 的 ";
        public static final String MISSING_SERVER_NAME_HEADER = CLIENT_PREFIX + "连接请求头中缺少服务器名，将断开连接";
        public static final String INVALID_CLIENT_ORIGIN_HEADER = CLIENT_PREFIX + "连接请求头中客户端来源错误，将断开连接";
        public static final String SERVER_NAME_PARSE_FAILED_HEADER = CLIENT_PREFIX + "连接请求头中服务器名解析失败，将断开连接";
        public static final String INVALID_ACCESS_TOKEN_HEADER = CLIENT_PREFIX + "连接身份验证失败，将断开连接";

        public static final String CLIENT_CONNECTED = CLIENT_PREFIX + "客户端已连接";
        public static final String CLIENT_DISCONNECTED = CLIENT_PREFIX + "客户端已断开";
        public static final String CLIENT_HAD_BEEN_DISCONNECTED = CLIENT_PREFIX + "客户端已被断开";

        public static final String CONNECTION_ERROR = CLIENT_PREFIX + "WebSocket 连接出现异常：%s";

        public static final String SERVER_STARTING = "WebSocket Server 在 %s:%s 启动...";
    }
}
