package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;

public class ReloadCommand extends SubCommand {

    /**
     * 获取命令名称
     *
     * @return reload
     */
    @Override
    public String getName() {
        return "reload";
    }

    /**
     * 获取命令描述
     *
     * @return 重载配置文件并重新连接所有 Websocket Client
     */
    @Override
    public String getDescription() {
        return "重载配置文件并重新连接所有 Websocket Client";
    }

    /**
     * 重载 WebSocket reload 命令调用
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    public void execute(Object commandReturner, java.util.List<String> args) {
        GlobalContext.executeReloadCommand(commandReturner);
    }
}

