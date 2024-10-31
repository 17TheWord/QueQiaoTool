package com.github.theword.queqiao.tool.config


data class WebSocketServerConfig(
    var enable: Boolean = true,
    var host: String = "127.0.0.1",
    var port: Int = 8080,
) {
    fun isEnable(): Boolean {
        return enable
    }
}
