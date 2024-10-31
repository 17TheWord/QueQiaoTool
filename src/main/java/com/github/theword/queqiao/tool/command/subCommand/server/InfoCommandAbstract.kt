package com.github.theword.queqiao.tool.command.subCommand.server

import com.github.theword.queqiao.tool.command.subCommand.ServerCommandAbstract
import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract
import com.github.theword.queqiao.tool.utils.Tool

abstract class InfoCommandAbstract : ServerCommandAbstract() {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return info
         */
        get() = "info"

    override val description: String
        /**
         * 获取命令描述
         *
         * @return 获取 Websocket Server 信息
         */
        get() = "获取 Websocket Server 信息"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 使用：/[ServerCommandAbstract.usage] info
         */
        get() = super.usage + " info"

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return [ClientCommandAbstract.permissionNode].info
         */
        get() = super.permissionNode + ".info"

    /**
     * 获取 WebSocket 服务端状态
     * 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     */
    override fun execute(commandReturner: Any?) {
        if (!Tool.config.websocketServer.isEnable()) {
            Tool.commandReturn(
                commandReturner,
                "Websocket Server 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketServer 配置项"
            )
            Tool.commandReturn(
                commandReturner,
                String.format(
                    "配置项中地址为 %s:%d",
                    Tool.config.websocketServer.host,
                    Tool.config.websocketServer.port
                )
            )
            return
        }

        val wsServer = Tool.websocketManager.wsServer

        if (wsServer == null) {
            Tool.commandReturn(commandReturner, "Websocket Server 为null，查询失败")
            return
        }

        Tool.commandReturn(
            commandReturner,
            String.format("当前 Websocket Server 已开启，监听地址为 %s:%d", wsServer.address.hostString, wsServer.port)
        )

        if (wsServer.connections.isEmpty()) {
            Tool.commandReturn(commandReturner, "当前暂无 Websocket 连接到该 Server")
            return
        }

        Tool.commandReturn(
            commandReturner,
            String.format("当前 Websocket Server 已有 %d 个连接", wsServer.connections.size)
        )

        var count = 0
        for (webSocket in wsServer.connections) {
            count++
            Tool.commandReturn(
                commandReturner,
                String.format(
                    "%d 来自 %s:%d 的连接",
                    count,
                    webSocket.remoteSocketAddress.hostString,
                    webSocket.remoteSocketAddress.port
                )
            )
        }
    }

    /**
     * 占位
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        execute(commandReturner)
    }
}
