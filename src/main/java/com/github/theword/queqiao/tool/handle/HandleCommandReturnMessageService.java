package com.github.theword.queqiao.tool.handle;

/**
 * 公共命令返回消息处理
 *
 * <p>服务端均需实现该接口
 */
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
