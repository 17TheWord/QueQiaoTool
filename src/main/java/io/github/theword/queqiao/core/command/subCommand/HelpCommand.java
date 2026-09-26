package io.github.theword.queqiao.core.command.subCommand;

import io.github.theword.queqiao.core.command.SubCommand;
import io.github.theword.queqiao.core.handle.HandleCommandReturnMessageService;
import org.slf4j.Logger;

import java.util.List;

public class HelpCommand extends SubCommand {

    public HelpCommand(HandleCommandReturnMessageService returnMessageService, Logger logger) {
        super(returnMessageService, logger);
    }

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
    protected void onExecute(Object commandReturner, List<String> args) {
        SubCommand root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        sendCommandTree(commandReturner, root);
    }
}
