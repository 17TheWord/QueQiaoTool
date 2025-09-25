package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class HelpCommandAbstract implements SubCommand {

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
     * 获取命令前缀
     *
     * <p>用于遍历时判断前驱后继
     *
     * <p>前缀为命令头则代表根命令
     *
     * @return {@link BaseConstant#COMMAND_HEADER}
     */
    @Override
    public String getPrefix() {
        return BaseConstant.COMMAND_HEADER;
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
     * 获取命令用法
     *
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} help
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER;
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link BaseConstant#COMMAND_HEADER}.help
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".help";
    }

    /**
     * 执行命令 获取所有命令使用方法
     *
     * @param commandReturner 命令执行者
     */
    public void execute(Object commandReturner) {
        // TODO 截取根目录使用方法
    }

    /**
     * 执行命令
     *
     * <p>Pass
     *
     * @param commandReturner 命令执行者
     * @param all             布尔值占位符
     */
    @Override
    public void execute(Object commandReturner, boolean all) {
        execute(commandReturner);
    }
}
