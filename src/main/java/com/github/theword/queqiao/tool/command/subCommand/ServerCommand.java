package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.command.subCommand.server.InfoCommand;

import java.util.List;

public class ServerCommand extends SubCommand {

    public ServerCommand() {
        addChild(new InfoCommand());
    }

    /**
     * 获取命令名称
     *
     * @return server
     */
    @Override
    public String getName() {
        return "server";
    }

    /**
     * 获取命令描述
     *
     * @return Websocket Server 命令
     */
    @Override
    public String getDescription() {
        return "Websocket Server 命令";
    }


    /**
     * 执行命令
     *
     * <p>位于本命令 pass
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        sendCommandTree(commandReturner, this);
    }
}
