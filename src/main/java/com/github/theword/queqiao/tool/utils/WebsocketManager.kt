package com.github.theword.queqiao.tool.utils

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage
import com.github.theword.queqiao.tool.websocket.WsClient
import com.github.theword.queqiao.tool.websocket.WsServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Consumer


class WebsocketManager {
    /**
     * Websocket Client 列表
     */
    val wsClientList: MutableList<WsClient> = ArrayList()

    /**
     * Websocket Server
     */
    var wsServer: WsServer? = null

    /**
     * 启动 WebSocket 客户端
     * 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private fun startWebsocketClients(commandReturner: Any) {
        if (Tool.config.websocketClient.isEnable()) {
            Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.LAUNCHING)
            Tool.config.websocketClient.urlList.forEach(Consumer { websocketUrl: String ->
                try {
                    val wsClient = WsClient(URI(websocketUrl))
                    wsClient.connect()
                    wsClientList.add(wsClient)
                } catch (e: URISyntaxException) {
                    Tool.commandReturn(
                        commandReturner,
                        String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl)
                    )
                }
            })
        }
    }

    /**
     * 停止 WebSocket 客户端
     * 关闭原因至少需要传入一个带有 %s 的字符串来填入对应的 URI
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    private fun stopWebsocketClients(code: Int, reason: String, commandReturner: Any?) {
        wsClientList.forEach(Consumer { wsClient: WsClient ->
            Tool.commandReturn(commandReturner, String.format(reason, wsClient.uri))
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.uri))
        })
        wsClientList.clear()
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST)
    }


    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    fun restartWebsocketClients(commandReturner: Any) {
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADING)
        stopWebsocketClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner)
        startWebsocketClients(commandReturner)
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADED)
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     */
    private fun startWebsocketServer(commandReturner: Any) {
        if (Tool.config.websocketServer.isEnable()) {
            wsServer = WsServer(InetSocketAddress(Tool.config.websocketServer.host, Tool.config.websocketServer.port))
            wsServer!!.start()
            Tool.commandReturn(
                commandReturner,
                String.format(
                    WebsocketConstantMessage.Server.SERVER_STARTING,
                    Tool.config.websocketServer.host,
                    Tool.config.websocketServer.port
                )
            )
        }
    }

    /**
     * 停止 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     * @param reason          原因
     */
    private fun stopWebsocketServer(commandReturner: Any?, reason: String) {
        if (wsServer != null) {
            try {
                wsServer!!.stop(0, reason)
                Tool.commandReturn(commandReturner, reason)
            } catch (e: InterruptedException) {
                Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING)
                Tool.debugLog(e.message)
            }
            wsServer = null
        }
    }

    /**
     * 重载 WebSocket 服务器
     * 目前只有通过reload命令调用重载
     *
     * @param commandReturner 命令执行者
     */
    fun restartWebsocketServer(commandReturner: Any) {
        stopWebsocketServer(commandReturner, WebsocketConstantMessage.Server.RELOADING)
        startWebsocketServer(commandReturner)
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.RELOADED)
    }

    /**
     * 启动 WebSocket
     * 开服时调用
     *
     * @param commandReturner 命令执行者
     */
    fun startWebsocket(commandReturner: Any) {
        startWebsocketClients(commandReturner)
        startWebsocketServer(commandReturner)
    }

    /**
     * 因 Minecraft Server 关闭，关闭 WebSocket
     * 关服时调用
     */
    fun stopWebsocketByServerClose() {
        stopWebsocket(1000, WebsocketConstantMessage.Client.CLOSING_CONNECTION, null)
    }

    /**
     * 停止 WebSocket
     * 除传入关闭码、关闭原因外，还需传入命令执行者（可为null）
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    fun stopWebsocket(code: Int, reason: String, commandReturner: Any?) {
        stopWebsocketClients(code, reason, commandReturner)
        stopWebsocketServer(commandReturner, reason)
    }
}
