package com.github.theword.queqiao.tool.command.subCommand

import com.github.theword.queqiao.tool.command.SubCommand
import com.github.theword.queqiao.tool.constant.BaseConstant

abstract class HelpCommandAbstract : SubCommand {
    override val name: String
        /**
         * 获取命令名称
         *
         * @return help
         */
        get() = "help"

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
         * @return 获取命令帮助
         */
        get() = "获取命令帮助"

    override val usage: String
        /**
         * 获取命令用法
         *
         * @return 使用：/[BaseConstant.COMMAND_HEADER] help
         */
        get() = "使用：/" + BaseConstant.COMMAND_HEADER

    override val permissionNode: String
        /**
         * 获取命令权限节点
         *
         * @return [BaseConstant.COMMAND_HEADER].help
         */
        get() = BaseConstant.COMMAND_HEADER + ".help"

    /**
     * 执行命令
     * 获取所有命令使用方法
     *
     * @param commandReturner 命令执行者
     */
    override fun execute(commandReturner: Any?) {
        // TODO 截取根目录使用方法
    }

    /**
     * 执行命令
     *
     * Pass
     *
     * @param commandReturner 命令执行者
     * @param boolVar             布尔值占位符
     */
    override fun execute(commandReturner: Any?, boolVar: Boolean) {
        execute(commandReturner)
    }
}
