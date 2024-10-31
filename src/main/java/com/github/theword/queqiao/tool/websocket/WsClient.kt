package com.github.theword.queqiao.tool.websocket

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage
import com.github.theword.queqiao.tool.utils.Tool
import lombok.SneakyThrows
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * WebSocket 客户端
 */
class WsClient @SneakyThrows constructor(uri: URI) : WebSocketClient(uri) {
    /**
     * 重连定时器
     */
    private val timer = Timer()

    /**
     * 处理协议消息
     */
    private val handleProtocolMessage = HandleProtocolMessage()
    private var reconnectTimes = 1

    /**
     * Websocket Client 构造函数
     *
     */
    init {
        addHeader("x-self-name", URLEncoder.encode(Tool.config.serverName, StandardCharsets.UTF_8.toString()))
        addHeader("x-client-origin", "minecraft")
        if (Tool.config.accessToken.isNotEmpty()) addHeader("Authorization", "Bearer " + Tool.config.accessToken)
    }

    /**
     * 连接打开时
     *
     * @param serverHandshake ServerHandshake
     */
    override fun onOpen(serverHandshake: ServerHandshake) {
        Tool.logger.info(String.format(WebsocketConstantMessage.Client.CONNECT_SUCCESSFUL, getURI()))
        reconnectTimes = 1
    }

    /**
     * 收到消息时触发
     * 向服务器游戏内公屏发送信息
     */
    override fun onMessage(message: String) {
        if (Tool.config.isEnable()) {
            val response = handleProtocolMessage.handleWebSocketJson(this, message)
            this.send(response.json)
        }
    }

    /**
     * 关闭时
     *
     * @param code   关闭码
     * @param reason 关闭信息
     * @param remote 是否关闭
     */
    override fun onClose(code: Int, reason: String, remote: Boolean) {
        if (remote && reconnectTimes <= Tool.config.websocketClient.reconnectMaxTimes) {
            reconnectWebsocket()
        }
    }

    /**
     * 重连
     * 延迟一定时间后重连
     */
    fun reconnectWebsocket() {
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                reconnect()
            }
        }
        timer.schedule(timerTask, Tool.config.websocketClient.reconnectInterval * 1000L)
    }

    /**
     * 关闭连接且不重连
     *
     * @param code   关闭码
     * @param reason 关闭信息
     */
    fun stopWithoutReconnect(code: Int, reason: String?) {
        timer.cancel()
        close(code, reason)
    }

    /**
     * 重连
     */
    override fun reconnect() {
        Tool.debugLog(String.format(WebsocketConstantMessage.Client.RECONNECTING, getURI(), reconnectTimes))
        reconnectTimes++
        super.reconnect()
        if (reconnectTimes == Tool.config.websocketClient.reconnectMaxTimes + 1) {
            Tool.logger.info(String.format(WebsocketConstantMessage.Client.MAX_RECONNECT_ATTEMPTS_REACHED, getURI()))
        }
    }

    /**
     * 触发异常时
     *
     * @param exception 所有异常
     */
    override fun onError(exception: Exception) {
        Tool.logger.warn(String.format(WebsocketConstantMessage.Client.CONNECTION_ERROR, getURI(), exception.message))
        if (exception is ConnectException && exception.message == "Connection refused: connect" && reconnectTimes <= Tool.config.websocketClient.reconnectMaxTimes) {
            reconnectWebsocket()
        }
    }

    /**
     * 发送消息
     * 如果连接已打开，则发送消息，否则打印错误信息
     *
     * @param text 消息
     */
    override fun send(text: String) {
        if (isOpen) {
            super.send(text)
            Tool.debugLog(String.format(WebsocketConstantMessage.Client.SEND_MESSAGE, getURI(), text))
        } else {
            Tool.debugLog(WebsocketConstantMessage.Client.SEND_MESSAGE_FAILED, getURI(), text)
        }
    }
}
