package com.github.theword.queqiao.tool.handle;

/**
 * 公共命令返回消息处理
 *
 * <p>服务端均需实现该接口
 */
public abstract class HandleCommandReturnMessageService {

    /**
     * 发送命令返回消息中间层
     * <p> 用于加一层防护，判断发送者是否为空等其他操作 </p>
     *
     * @param commandReturner 命令返回者
     * @param message         返回消息
     */
    public void sendReturnMessage(Object commandReturner, String message) {
        if (commandReturner == null) return;
        handleCommandReturnMessage(commandReturner, message);
    }

    /**
     * 处理命令返回消息
     *
     * @param commandReturner 命令返回者
     * @param message         返回消息
     */
    public abstract void handleCommandReturnMessage(Object commandReturner, String message);

    /**
     * 判断是否拥有权限
     *
     * @param commandReturner 命令返回者
     * @param permissionNode  权限节点
     * @return 是否拥有权限
     */
    public abstract boolean hasPermission(Object commandReturner, String permissionNode);
}
