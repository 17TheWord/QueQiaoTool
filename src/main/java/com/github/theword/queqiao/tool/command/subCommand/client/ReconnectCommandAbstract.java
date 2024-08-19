package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class ReconnectCommandAbstract implements SubCommand {

    /**
     * @return reconnect
     */
    @Override
    public String getName() {
        return "reconnect";
    }

    /**
     * @return 重新连接 Websocket Clients。
     */
    @Override
    public String getDescription() {
        return "重新连接 Websocket Clients。";
    }

    /**
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} client reconnect [all]
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " client reconnect [all]";
    }
}
