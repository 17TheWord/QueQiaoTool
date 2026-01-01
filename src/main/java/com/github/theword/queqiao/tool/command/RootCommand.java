package com.github.theword.queqiao.tool.command;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.subCommand.ClientCommand;
import com.github.theword.queqiao.tool.command.subCommand.HelpCommand;
import com.github.theword.queqiao.tool.command.subCommand.ReloadCommand;
import com.github.theword.queqiao.tool.command.subCommand.ServerCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstant;

import java.util.List;

/**
 * 根命令抽象类
 *
 * <p>所有平台的根命令实现应继承此类
 * <p>在各平台的实现中注册所有一级子命令
 *
 * @since 0.5.0
 */
public class RootCommand extends SubCommand {

    /**
     * 构造根命令
     *
     */
    public RootCommand() {
        addChild(new HelpCommand());
        addChild(new ReloadCommand());
        addChild(new ServerCommand());
        addChild(new ClientCommand());
    }

    /**
     * 获取命令名称
     *
     * @return 命令名称（queqiao）
     */
    @Override
    public String getName() {
        return BaseConstant.COMMAND_HEADER;
    }

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    @Override
    public String getDescription() {
        return "QueQiao Tool 主命令";
    }

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                commandReturner, "请使用帮助命令查看可用子命令：" + BaseConstant.COMMAND_HEADER + " help"
        );
    }
}
