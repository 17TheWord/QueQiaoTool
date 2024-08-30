package com.github.theword.queqiao.tool.command.subCommand.server;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class InfoCommandAbstract implements SubCommand {

    /**
     * 获取命令名称
     *
     * @return Websocket Server Info
     */
    @Override
    public String getName() {
        return "Websocket Server Info";
    }

    /**
     * 获取命令描述
     *
     * @return Websocket Server Info
     */
    @Override
    public String getDescription() {
        return "获取 Websocket Server 信息";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} server info
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " server info";
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link BaseConstant#COMMAND_HEADER}.server.info
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".server.info";
    }
}
