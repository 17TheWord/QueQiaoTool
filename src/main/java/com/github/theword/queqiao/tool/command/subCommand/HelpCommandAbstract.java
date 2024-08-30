package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class HelpCommandAbstract implements SubCommand {

    /**
     * 获取命令名称
     *
     * @return help
     */
    @Override
    public String getName() {
        return "help";
    }

    /**
     * 获取命令描述
     *
     * @return 获取命令帮助
     */
    @Override
    public String getDescription() {
        return "获取命令帮助";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} help
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER;
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link BaseConstant#COMMAND_HEADER}.help
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".help";
    }
}
