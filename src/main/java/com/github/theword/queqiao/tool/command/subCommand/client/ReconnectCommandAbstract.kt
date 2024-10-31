package com.github.theword.queqiao.tool.command.subCommand.client

import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract
import com.github.theword.queqiao.tool.constant.CommandConstantMessage
import com.github.theword.queqiao.tool.utils.Tool
import com.github.theword.queqiao.tool.websocket.WsClient
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

abstract class ReconnectCommandAbstract : ClientCommandAbstract() {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return reconnect
         */
        get() = "reconnect"

    override val description: String
        /**
         * 获取命令描述
         *
         * @return 重新连接 Websocket Clients
         */
        get() = "重新连接 Websocket Clients"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 使用：/[ClientCommandAbstract.usage] reconnect [all]
         */
        get() = super.usage + " reconnect [all]"

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return [ClientCommandAbstract.permissionNode].reconnect
         */
        get() = super.permissionNode + ".reconnect"

    /**
     * 执行命令
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     */
    override fun execute(commandReturner: Any?) {
        execute(commandReturner, false)
    }

    /**
     * 重连 WebSocket 客户端
     * reconnect [boolVar] 命令调用
     *
     * @param commandReturner 命令执行者
     * @param boolVar             是否全部重连
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        val reconnectCount =
            if (boolVar) CommandConstantMessage.RECONNECT_ALL_CLIENT else CommandConstantMessage.RECONNECT_NOT_OPEN_CLIENT
        Tool.commandReturn(commandReturner, reconnectCount)

        val opened = AtomicInteger()

        val wsClientList: List<WsClient> = Tool.websocketManager.wsClientList

        wsClientList.forEach(Consumer { wsClient: WsClient ->
            if (boolVar || !wsClient.isOpen) {
                wsClient.reconnectWebsocket()
                Tool.commandReturn(
                    commandReturner, String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.uri)
                )
            } else {
                opened.getAndIncrement()
            }
        })
        if (opened.get() == wsClientList.size) {
            Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT)
        }
        Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECTED)
    }
}
