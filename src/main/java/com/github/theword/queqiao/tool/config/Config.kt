package com.github.theword.queqiao.tool.config

import com.github.theword.queqiao.tool.utils.Tool

/**
 * 配置文件
 */
class Config(isModServer: Boolean) : CommonConfig() {
    /**
     * 是否启用组件
     */
    var enable = true

    /**
     * 是否开启调试模式
     */
    var debug = false

    /**
     * 服务器名
     */
    var serverName: String = "Server"

    /**
     * 访问令牌
     */
    var accessToken: String = ""

    /**
     * 消息前缀
     */
    var messagePrefix: String = "鹊桥"

    /**
     * WebSocket Server 配置项
     */
    var websocketServer = WebSocketServerConfig()

    /**
     * WebSocket Client 配置项
     */
    var websocketClient = WebSocketClientConfig()

    /**
     * 订阅事件配置项
     */
    var subscribeEvent = SubscribeEventConfig()

    fun isEnable(): Boolean {
        return enable
    }

    fun isDebug(): Boolean {
        return enable
    }

    init {
        val configFolder = if (isModServer) "config" else "plugins"
        val serverType = if (isModServer) "模组" else "插件"
        Tool.logger.info("当前服务端类型为：{}服", serverType)
        readConfigFile(configFolder, "config.yml")
    }

    override fun loadConfigValues(configMap: Map<String, Any>) {
        enable = configMap["enable"] as Boolean
        debug = configMap["debug"] as Boolean
        serverName = configMap["server_name"] as String
        accessToken = configMap["access_token"] as String
        messagePrefix = configMap["message_prefix"] as String

        loadWebsocketServerConfig(configMap)
        loadWebsocketClientConfig(configMap)
        loadSubscribeEventConfig(configMap)
        Tool.config = this
    }


    @Suppress("UNCHECKED_CAST")
    private fun loadWebsocketServerConfig(configMap: Map<String, Any>) {
        val websocketServerConfig = configMap["websocket_server"] as Map<String, Any>
        websocketServer.enable = websocketServerConfig["enable"] as Boolean
        val host = websocketServerConfig["host"] as String?
        if (host == "0.0.0.0" || host == "127.0.0.1" || host == "localhost") websocketServer.host =
            (websocketServerConfig["host"] as String?)!!
        else {
            websocketServer.host = "127.0.0.1"
            Tool.logger.warn("哪有你这么设置IP的？你确定你改的host是对的？？我已经帮你改到 127.0.0.1 了，好好想想再去改host！！！")
        }
        websocketServer.port = websocketServerConfig["port"] as Int
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadWebsocketClientConfig(configMap: Map<String, Any>) {
        val websocketClientConfig = configMap["websocket_client"] as Map<String, Any>
        websocketClient.enable = websocketClientConfig["enable"] as Boolean
        websocketClient.reconnectInterval = websocketClientConfig["reconnect_interval"] as Int
        websocketClient.reconnectMaxTimes = websocketClientConfig["reconnect_max_times"] as Int
        websocketClient.urlList = websocketClientConfig["url_list"] as List<String>
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadSubscribeEventConfig(configMap: Map<String, Any>) {
        val subscribeEventConfig = configMap["subscribe_event"] as Map<String, Any>
        subscribeEvent.playerChat = subscribeEventConfig["player_chat"] as Boolean
        subscribeEvent.playerCommand = subscribeEventConfig["player_command"] as Boolean
        subscribeEvent.playerDeath = subscribeEventConfig["player_death"] as Boolean
        subscribeEvent.playerJoin = subscribeEventConfig["player_join"] as Boolean
        subscribeEvent.playerQuit = subscribeEventConfig["player_quit"] as Boolean
    }

    companion object {
        @JvmStatic
        fun loadConfig(isModServer: Boolean): Config {
            return Config(isModServer)
        }
    }
}
