package com.github.theword.queqiao.tool.constant

object WebsocketConstantMessage {
    const val PARSE_MESSAGE_ERROR_ON_MESSAGE: String = "解析来自 %s 的 WebSocket 消息时出现异常"
    const val CLOSE_BY_RELOAD: String = "Websocket 正在重载"


    object Client {
        const val SEND_MESSAGE: String = "发送至 %s 的消息：%s"
        const val LAUNCHING: String = "WebSocket Client 正在启动..."
        const val CONNECT_SUCCESSFUL: String = "已成功连接至 %s 的 WebSocket 服务器！"
        const val CLEAR_WEBSOCKET_CLIENT_LIST: String = "已清空 Websocket Client 列表。"
        const val RELOADING: String = "Websocket Client 正在重载"
        const val RELOADED: String = "Websocket Client 重载完毕"
        private const val CONNECT_TO = "连接至：%s 的 "
        const val CLOSING_CONNECTION: String = CONNECT_TO + "WebSocket Client 正在关闭。"
        const val RECONNECTING: String = CONNECT_TO + "WebSocket 连接已断开，尝试第 %s 次重连..."
        const val CONNECTION_ERROR: String = CONNECT_TO + "WebSocket 连接出现异常：%s"
        const val CONNECTION_NOT_OPEN: String = CONNECT_TO + "WebSocket 连接未打开"
        const val SEND_MESSAGE_FAILED: String = CONNECTION_NOT_OPEN + "，发送消息 %s 失败"
        const val URI_SYNTAX_ERROR: String = CONNECT_TO + "WebSocket URL 格式错误，无法连接！"
        const val MAX_RECONNECT_ATTEMPTS_REACHED: String =
            CONNECT_TO + "重连次数达到最大值，将不再自动重连，请使用命令手动重连！"
    }

    object Server {
        const val ERROR_ON_STOPPING: String = "Websocket Server 正在启动时出现异常。"

        const val BROADCAST_MESSAGE: String = "向所有客户端广播消息：%s"
        const val RELOADING: String = "Websocket Server 正在重载"
        const val RELOADED: String = "Websocket Server 重载完毕"
        const val SERVER_STARTING: String = "WebSocket Server 在 %s:%s 启动..."
        private const val CLIENT_PREFIX = "来自：%s 的 "
        const val MISSING_SERVER_NAME_HEADER: String = CLIENT_PREFIX + "连接请求头中缺少服务器名，将断开连接"
        const val INVALID_CLIENT_ORIGIN_HEADER: String = CLIENT_PREFIX + "连接请求头中客户端来源错误，将断开连接"
        const val SERVER_NAME_PARSE_FAILED_HEADER: String = CLIENT_PREFIX + "连接请求头中服务器名解析失败，将断开连接"
        const val INVALID_SERVER_NAME_HEADER: String = CLIENT_PREFIX + "连接请求头中服务器名：%s 错误，将断开连接"
        const val INVALID_ACCESS_TOKEN_HEADER: String = CLIENT_PREFIX + "连接身份验证码：%s 失败，将断开连接"
        const val CLIENT_CONNECTED: String = CLIENT_PREFIX + "客户端已连接"
        const val CLIENT_DISCONNECTED: String = CLIENT_PREFIX + "客户端已断开"
        const val CLIENT_HAD_BEEN_DISCONNECTED: String = CLIENT_PREFIX + "客户端已被断开"
        const val CONNECTION_ERROR: String = CLIENT_PREFIX + "WebSocket 连接出现异常：%s"
    }
}
