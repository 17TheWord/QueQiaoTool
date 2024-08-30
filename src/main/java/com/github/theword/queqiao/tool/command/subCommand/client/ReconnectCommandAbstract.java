package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class ReconnectCommandAbstract implements SubCommand {

    /**
     * 获取命令名称
     *
     * @return reconnect
     */
    @Override
    public String getName() {
        return "reconnect";
    }

    /**
     * 获取命令描述
     *
     * @return 重新连接 Websocket Clients。
     */
    @Override
    public String getDescription() {
        return "重新连接 Websocket Clients。";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} client reconnect [all]
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " client reconnect [all]";
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link BaseConstant#COMMAND_HEADER}.client.reconnect
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".client.reconnect";
    }
}
