package com.github.theword.queqiao.tool.command;

/**
 * 子命令接口
 *
 * <p>所有命令均需实现该接口下对应的Abstract子类
 */
public interface SubCommand {

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    String getName();

    /**
     * 获取命令前缀
     *
     * <p>用于遍历时判断前驱后继
     *
     * <p>为空字符串则代表根命令
     *
     * @return 命令前缀
     */
    String getPrefix();

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

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     * @since 0.1.5
     */
    void execute(Object commandReturner, boolean boolVar);

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @since 0.1.5
     */
    void execute(Object commandReturner);
}
