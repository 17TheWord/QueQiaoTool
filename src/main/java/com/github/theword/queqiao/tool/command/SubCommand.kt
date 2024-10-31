package com.github.theword.queqiao.tool.command

interface SubCommand {
    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    val name: String

    /**
     * 获取命令前缀
     *
     * 用于遍历时判断前驱后继
     * <P>为空字符串则代表根命令</P>
     *
     * @return 命令前缀
     */
    val prefix: String

    /**
     * 获取命令描述
     *
     * @return 命令描述
     */
    val description: String

    /**
     * 获取命令用法
     *
     * @return 命令用法
     */
    val usage: String

    /**
     * 获取命令权限节点
     *
     * @return 权限节点
     */
    val permissionNode: String

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     * @since 0.1.5
     */
    fun execute(commandReturner: Any?, boolVar: Boolean)

    /**
     * 执行命令
     *
     * @param commandReturner 命令执行者
     * @since 0.1.5
     */
    fun execute(commandReturner: Any?)
}
