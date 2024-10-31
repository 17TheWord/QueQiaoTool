package com.github.theword.queqiao.tool.command.subCommand

import com.github.theword.queqiao.tool.command.SubCommand
import com.github.theword.queqiao.tool.constant.BaseConstant

open class ClientCommandAbstract : SubCommand {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return client
         */
        get() = "client"

    override val prefix: String
        /**
         * 获取命令前缀
         *
         * 用于遍历时判断前驱后继
         * <P>为空字符串则代表根命令</P>
         *
         * @return [BaseConstant.COMMAND_HEADER]
         */
        get() = BaseConstant.COMMAND_HEADER

    override val description: String
        /**
         * 获取命令描述
         *
         * @return Websocket Client 命令
         */
        get() = "Websocket Client 命令"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 使用：/[BaseConstant.COMMAND_HEADER] client
         */
        get() = "使用：/" + BaseConstant.COMMAND_HEADER + " client"

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return [BaseConstant.COMMAND_HEADER].client
         */
        get() = BaseConstant.COMMAND_HEADER + ".client"

    /**
     * 执行命令
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        // pass
    }

    /**
     * 执行命令
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     */
    override fun execute(commandReturner: Any?) {
        // pass
    }
}
