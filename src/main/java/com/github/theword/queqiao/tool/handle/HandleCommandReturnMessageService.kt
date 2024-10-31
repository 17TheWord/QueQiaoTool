package com.github.theword.queqiao.tool.handle

interface HandleCommandReturnMessageService {
    /**
     * 处理命令返回消息
     *
     * @param commandReturner 命令返回者
     * @param message         返回消息
     */
    fun handleCommandReturnMessage(commandReturner: Any, message: String)

    /**
     * 判断是否拥有权限
     *
     * @param commandReturner 命令返回者
     * @param permissionNode  权限节点
     * @return 是否拥有权限
     */
    fun hasPermission(commandReturner: Any, permissionNode: String): Boolean
}
