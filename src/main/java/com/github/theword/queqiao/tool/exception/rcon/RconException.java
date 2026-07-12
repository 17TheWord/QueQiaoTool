package com.github.theword.queqiao.tool.exception.rcon;

import com.github.theword.queqiao.tool.exception.QueQiaoException;

public class RconException extends QueQiaoException {
    public RconException(String message) {
        super(message);
    }

    public RconException(String message, Throwable cause) {
        super(message, cause);
    }

    public static RconException disabled() {
        return new RconException("Rcon 功能未在配置文件中启用");
    }

    public static RconException disconnected() {
        return new RconException("Rcon 客户端未连接或已关闭");
    }

    public static RconException clientDisconnected() {
        return new RconException("Rcon 未连接");
    }

    public static RconException commandFailed(Throwable cause) {
        return new RconException("Rcon 命令执行失败", cause);
    }
}
