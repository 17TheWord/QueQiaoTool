package com.github.theword.queqiao.tool.command;

public interface SubCommand {

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    String getName();

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    String getDescription();

    /**
     * 获取命令用法
     *
     * @return 命令用法
     */
    String getUsage();

    /**
     * 获取命令权限节点
     *
     * @return 权限节点
     */
    String getPermissionNode();
}
