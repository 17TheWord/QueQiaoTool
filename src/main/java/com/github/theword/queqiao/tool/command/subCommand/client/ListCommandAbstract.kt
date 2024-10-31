package com.github.theword.queqiao.tool.command.subCommand.client

import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract
import com.github.theword.queqiao.tool.utils.Tool
import com.github.theword.queqiao.tool.websocket.WsClient

abstract class ListCommandAbstract : ClientCommandAbstract() {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return list
         */
        get() = "list"

    override val prefix: String
        /**
         * 获取命令前缀
         *
         * 用于遍历时判断前驱后继
         * <P>前缀为命令头则代表根命令</P>
         *
         * @return client
         */
        get() = "client"

    override val description: String
        /**
         * 获取命令描述
         *
         * @return 获取当前 Websocket Client 列表
         */
        get() = "获取当前 Websocket Client 列表"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 命令用法 使用：/[ClientCommandAbstract.usage] list
         */
        get() = super.usage + " list"

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return 权限节点 [ClientCommandAbstract.permissionNode].list
         */
        get() = super.permissionNode + ".list"

    /**
     * 获取 WebSocket 客户端状态
     * 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     * @since 0.1.5
     */
    override fun execute(commandReturner: Any?) {
        if (!Tool.config.websocketClient.isEnable()) {
            Tool.commandReturn(
                commandReturner,
                "Websocket Client 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketClient 配置项"
            )
            Tool.commandReturn(
                commandReturner,
                "配置文件中连接列表如下共 " + Tool.config.websocketClient.urlList.size + " 个 Client"
            )
            for (i in Tool.config.websocketClient.urlList.indices) {
                Tool.commandReturn(
                    commandReturner,
                    String.format("%d 连接至 %s", i + 1, Tool.config.websocketClient.urlList[i])
                )
            }
            return
        }

        val wsClientList: List<WsClient> = Tool.websocketManager.wsClientList

        Tool.commandReturn(commandReturner, "Websocket Client 列表，共 " + wsClientList.size + " 个 Client")

        for (i in wsClientList.indices) {
            val wsClient = wsClientList[i]
            Tool.commandReturn(
                commandReturner,
                String.format(
                    "%d 连接至 %s 的 Client，状态：%s",
                    i,
                    wsClient.uri,
                    if (wsClient.isOpen) "已连接" else "未连接"
                )
            )
        }
    }

    /**
     * 获取 WebSocket 客户端状态
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     * @since 0.1.5
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        execute(commandReturner)
    }
}
