package com.github.theword.queqiao.tool.command.subCommand

import com.github.theword.queqiao.tool.command.SubCommand
import com.github.theword.queqiao.tool.config.Config.Companion.loadConfig
import com.github.theword.queqiao.tool.constant.BaseConstant
import com.github.theword.queqiao.tool.constant.CommandConstantMessage
import com.github.theword.queqiao.tool.utils.Tool

abstract class ReloadCommandAbstract : SubCommand {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return reload
         */
        get() = "reload"

    override val prefix: String
        /**
         * 获取命令前缀
         *
         * 用于遍历时判断前驱后继
         * <P>前缀为命令头则代表根命令</P>
         *
         * @return [BaseConstant.COMMAND_HEADER]
         */
        get() = BaseConstant.COMMAND_HEADER

    override val description: String
        /**
         * 获取命令描述
         *
         * @return 重载配置文件并重新连接所有 Websocket Client
         */
        get() = "重载配置文件并重新连接所有 Websocket Client"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 使用：/[BaseConstant.COMMAND_HEADER] reload
         */
        get() = "使用：/" + BaseConstant.COMMAND_HEADER + " reload"

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return 权限节点
         */
        get() = BaseConstant.COMMAND_HEADER + ".reload"

    override fun execute(commandReturner: Any?) {
        execute(commandReturner, false)
    }

    /**
     * 重载 WebSocket
     * reload 命令调用
     *
     * @param boolVar     是否为 ModServer
     * @param commandReturner 命令执行者
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        Tool.config = loadConfig(boolVar)
        Tool.commandReturn(commandReturner, CommandConstantMessage.RELOAD_CONFIG)
        Tool.websocketManager.restartWebsocketServer(commandReturner!!)
        Tool.websocketManager.restartWebsocketClients(commandReturner)
    }
}
