package com.github.theword.queqiao.tool.constant

object CommandConstantMessage {
    const val RELOAD_CONFIG: String = "配置文件重载完成"

    const val RECONNECT_MESSAGE: String = "正在尝试重连至：%s 的 WebSocket 服务器..."
    const val RECONNECT_NOT_OPEN_CLIENT: String = "即将重新连接未处于开启状态的 Websocket Client..."
    const val RECONNECT_ALL_CLIENT: String = "即将重新连接所有的 Websocket Client..."
    const val RECONNECT_NO_CLIENT_NEED_RECONNECT: String = "没有需要重连的 Websocket Client"
    const val RECONNECTED: String = "重连任务已完成"
}
