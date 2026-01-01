package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.command.SubCommand;
import java.util.List;

public class ReconnectAllCommand extends SubCommand {

    /**
     * 获取命令名称
     *
     * @return all
     */
    @Override
    public String getName() {
        return "all";
    }

    /**
     * 获取命令描述
     *
     * @return 重连所有客户端
     */
    @Override
    public String getDescription() {
        return "强制重连所有客户端";
    }

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        ReconnectCommand.reconnect(commandReturner, true);
    }
}

