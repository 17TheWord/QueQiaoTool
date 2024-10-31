package com.github.theword.queqiao.tool.websocket

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage
import com.github.theword.queqiao.tool.utils.Tool
import lombok.SneakyThrows
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WsServer(address: InetSocketAddress) : WebSocketServer(address) {
    private val hostName: String = address.hostName
    private val port = address.port
    private val handleProtocolMessage = HandleProtocolMessage()

    /**
     * 获取客户端地址
     *
     * @param webSocket 客户端
     * @return 客户端地址
     */
    private fun getClientAddress(webSocket: WebSocket): String {
        return webSocket.remoteSocketAddress.toString().replaceFirst("/".toRegex(), "")
    }

    /**
     * 当客户端连接时执行
     * 连接将依次检验 x-self-name；x-client-origin；Authorization字段
     *
     * @param webSocket       客户端
     * @param clientHandshake 客户端握手信息
     */
    @SneakyThrows
    override fun onOpen(webSocket: WebSocket, clientHandshake: ClientHandshake) {
        val originServerName = clientHandshake.getFieldValue("x-self-name")
        if (originServerName.isEmpty()) {
            Tool.logger.warn(
                String.format(
                    WebsocketConstantMessage.Server.MISSING_SERVER_NAME_HEADER,
                    getClientAddress(webSocket)
                )
            )
            webSocket.close(1008, "Missing X-Self-name Header")
            return
        }

        val clientOrigin = clientHandshake.getFieldValue("x-client-origin")
        if (clientOrigin.equals("minecraft", ignoreCase = true)) {
            Tool.logger.warn(
                String.format(
                    WebsocketConstantMessage.Server.INVALID_CLIENT_ORIGIN_HEADER,
                    getClientAddress(webSocket)
                )
            )
            webSocket.close(1008, "X-Client-Origin Header cannot be minecraft")
            return
        }

        val serverName = URLDecoder.decode(originServerName, StandardCharsets.UTF_8.toString())
        if (serverName.isEmpty()) {
            Tool.logger.warn(
                String.format(
                    WebsocketConstantMessage.Server.SERVER_NAME_PARSE_FAILED_HEADER,
                    getClientAddress(webSocket)
                )
            )
            webSocket.close(1008, "X-Self-name Header cannot be empty")
            return
        }

        if (serverName != Tool.config.serverName) {
            Tool.logger.warn(
                String.format(
                    WebsocketConstantMessage.Server.INVALID_SERVER_NAME_HEADER,
                    getClientAddress(webSocket),
                    serverName
                )
            )
            webSocket.close(1008, "X-Self-name Header is wrong")
            return
        }

        val accessToken = clientHandshake.getFieldValue("Authorization")
        if (Tool.config.accessToken.isNotEmpty() && accessToken != "Bearer " + Tool.config.accessToken) {
            Tool.logger.warn(
                String.format(
                    WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER,
                    getClientAddress(webSocket),
                    accessToken
                )
            )
            webSocket.close(1008, "Authorization Header is wrong")
            return
        }

        Tool.logger.info(String.format(WebsocketConstantMessage.Server.CLIENT_CONNECTED, getClientAddress(webSocket)))
    }

    /**
     * 当客户端断开连接时执行
     *
     * @param webSocket 客户端
     * @param code      关闭码
     * @param reason    关闭原因
     * @param remote    是否是远程关闭
     */
    override fun onClose(webSocket: WebSocket, code: Int, reason: String, remote: Boolean) {
        val closeReason =
            if (remote) WebsocketConstantMessage.Server.CLIENT_DISCONNECTED else WebsocketConstantMessage.Server.CLIENT_HAD_BEEN_DISCONNECTED
        Tool.logger.info(String.format(closeReason, getClientAddress(webSocket)))
    }

    /**
     * 当接收到客户端的消息时执行
     *
     * @param webSocket 客户端
     * @param message   消息
     */
    override fun onMessage(webSocket: WebSocket, message: String) {
        if (Tool.config.isEnable()) {
            val response = handleProtocolMessage.handleWebSocketJson(webSocket, message)
            webSocket.send(response.json)
        }
    }

    /**
     * 当连接出现异常时执行
     *
     * @param webSocket 客户端
     * @param exception 异常
     */
    override fun onError(webSocket: WebSocket, exception: Exception) {
        Tool.logger.warn(
            String.format(
                WebsocketConstantMessage.Server.CONNECTION_ERROR,
                getClientAddress(webSocket),
                exception.message
            )
        )
    }

    /**
     * 当服务器启动时执行
     */
    override fun onStart() {
        Tool.logger.info(String.format(WebsocketConstantMessage.Server.SERVER_STARTING, hostName, port))
    }

    /**
     * 广播消息
     * 发送内容在debugLog中
     *
     * @param text 消息
     */
    override fun broadcast(text: String) {
        super.broadcast(text)
        Tool.debugLog(String.format(WebsocketConstantMessage.Server.BROADCAST_MESSAGE, text))
    }
}
