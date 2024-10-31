package com.github.theword.queqiao.tool.config


class WebSocketClientConfig(
    /**
     * 是否启用 WebSocket Client
     */
    var enable: Boolean = false,

    /**
     * 重连间隔
     */
    var reconnectInterval: Int = 5,

    /**
     * 最大重连次数
     */
    var reconnectMaxTimes: Int = 5,

    /**
     * WebSocket URL 列表
     */
    var urlList: List<String> = ArrayList(),
) {
    fun isEnable(): Boolean {
        return enable
    }
}