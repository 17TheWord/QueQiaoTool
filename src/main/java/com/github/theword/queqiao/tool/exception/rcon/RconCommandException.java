package com.github.theword.queqiao.tool.exception.rcon;

import com.github.theword.queqiao.tool.exception.QueQiaoToolException;

/**
 * Rcon 命令执行异常。
 */
public class RconCommandException extends QueQiaoToolException {
    private final Reason reason;

    private RconCommandException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private RconCommandException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    private static RconCommandException of(Reason reason, String message) {
        return new RconCommandException(reason, message);
    }

    private static RconCommandException of(Reason reason, String message, Throwable cause) {
        return new RconCommandException(reason, message, cause);
    }

    public static RconCommandException disabled() {
        return of(Reason.DISABLED, "Rcon 功能未在配置文件中启用");
    }

    public static RconCommandException notConnected() {
        return of(Reason.NOT_CONNECTED, "Rcon 客户端未连接或已关闭");
    }

    public static RconCommandException failed(Throwable cause) {
        String message = cause.getMessage() != null ? cause.getMessage() : "Rcon 命令执行失败";
        return of(Reason.FAILED, message, cause);
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        DISABLED,
        NOT_CONNECTED,
        FAILED
    }
}
