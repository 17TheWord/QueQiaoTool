package com.github.theword.queqiao.commands;

public interface SubCommand {

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
}
