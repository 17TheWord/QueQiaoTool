package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;

import java.util.List;

public class HelpCommand extends SubCommand {

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
     * 执行命令 获取所有命令使用方法
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    public void execute(Object commandReturner, List<String> args) {
        SubCommand root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, "========== 鹊桥帮助 ==========");
        sendCommandTree(commandReturner, root);
    }
}

