package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class ListCommandAbstract implements SubCommand {
    /**
     * 获取命令名称
     *
     * @return list
     */
    @Override
    public String getName() {
        return "list";
    }

    /**
     * 获取命令描述
     *
     * @return list
     */
    @Override
    public String getDescription() {
        return "获取当前 Websocket Client 列表";
    }

    /**
     * 获取命令用法
     *
     * @return 命令用法 使用：/{@link BaseConstant#COMMAND_HEADER} client list
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " client list";
    }

    /**
     * 获取命令权限节点
     *
     * @return 权限节点
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".client.list";
    }
}
