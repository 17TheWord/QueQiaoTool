package com.github.theword.queqiao.commands.subCommands.client;

import com.github.theword.queqiao.commands.SubCommand;
import com.github.theword.queqiao.constant.BaseConstant;

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
     * @return 使用：/BaseConstant.COMMAND_HEADER client reconnect [all]
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " client reconnect [all]";
    }
}
