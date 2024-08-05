package com.github.theword.queqiao.handle;

public interface HandleCommandReturnMessage {

    /**
     * 处理命令返回消息
     *
     * @param commandReturner 命令返回者
     * @param message         返回消息
     */
    void handleCommandReturnMessage(Object commandReturner, String message);
}
