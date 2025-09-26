package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public abstract class ReloadCommandAbstract implements SubCommand {

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
     * @return 重载配置文件并重新连接所有 Websocket Client
     */
    @Override
    public String getDescription() {
        return "重载配置文件并重新连接所有 Websocket Client";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link BaseConstant#COMMAND_HEADER} reload
     */
    @Override
    public String getUsage() {
        return "使用：/" + BaseConstant.COMMAND_HEADER + " reload";
    }

    /**
     * 获取命令权限节点
     *
     * @return 权限节点
     */
    @Override
    public String getPermissionNode() {
        return BaseConstant.COMMAND_HEADER + ".reload";
    }

    @Override
    public void execute(Object commandReturner) {
        execute(commandReturner, false);
    }

    /**
     * 重载 WebSocket reload 命令调用
     *
     * @param isModServer     是否为 ModServer
     * @param commandReturner 命令执行者
     */
    @Override
    public void execute(Object commandReturner, boolean isModServer) {
        GlobalContext.executeReloadCommand(commandReturner, isModServer);
    }
}
