package com.github.theword.queqiao.tool.handle;

public interface HandleCommandReturnMessageService {

    /**
     * 处理命令返回消息
     *
     * @param commandReturner 命令返回者
     * @param message         返回消息
     */
    void handleCommandReturnMessage(Object commandReturner, String message);

    /**
     * 判断是否拥有权限
     *
     * @param commandReturner 命令返回者
     * @param permissionNode  权限节点
     * @return 是否拥有权限
     */
    boolean hasPermission(Object commandReturner, String permissionNode);
}
