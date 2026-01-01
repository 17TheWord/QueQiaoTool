package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.command.subCommand.client.ListCommand;
import com.github.theword.queqiao.tool.command.subCommand.client.ReconnectCommand;

public class ClientCommand extends SubCommand {

    public ClientCommand() {
        // 注册子命令
        addChild(new ListCommand());
        addChild(new ReconnectCommand());
    }

    /**
     * 获取命令名称
     *
     * @return client
     */
    @Override
    public String getName() {
        return "client";
    }

    /**
     * 获取命令描述
     *
     * @return Websocket Client 命令
     */
    @Override
    public String getDescription() {
        return "Websocket Client 命令";
    }


    /**
     * 执行命令
     *
     * <p>Pass
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, java.util.List<String> args) {
        sendCommandTree(commandReturner, this);
    }
}

