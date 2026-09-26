package io.github.theword.queqiao.core.exception.rcon;

import io.github.theword.queqiao.core.exception.QueQiaoException;

/**
 * Rcon 相关异常
 *
 * <p>携带 {@link Kind} 以便调用方区分失败性质并映射到合适的协议状态码：
 * 未启用/未连接属于"服务暂不可用"，命令执行失败属于服务端错误，
 * 二者不应被统一当作调用方的请求格式错误。
 */
public class RconException extends QueQiaoException {

    /**
     * 失败性质
     */
    public enum Kind {

        /**
         * Rcon 功能未在配置中启用
         */
        DISABLED,

        /**
         * Rcon 客户端未连接或已关闭
         */
        DISCONNECTED,

        /**
         * 命令为空或仅包含空白
         */
        INVALID_COMMAND,

        /**
         * 命令已下发但执行失败
         */
        COMMAND_FAILED
    }

    private final Kind kind;

    public RconException(String message) {
        this(message, null, Kind.COMMAND_FAILED);
    }

    public RconException(String message, Throwable cause) {
        this(message, cause, Kind.COMMAND_FAILED);
    }

    private RconException(String message, Throwable cause, Kind kind) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * 获取失败性质
     *
     * @return 失败性质，永不为 null
     */
    public Kind getKind() {
        return kind;
    }

    public static RconException disabled() {
        return new RconException("Rcon 功能未在配置文件中启用", null, Kind.DISABLED);
    }

    public static RconException disconnected() {
        return new RconException("Rcon 客户端未连接或已关闭", null, Kind.DISCONNECTED);
    }

    public static RconException clientDisconnected() {
        return new RconException("Rcon 未连接", null, Kind.DISCONNECTED);
    }

    public static RconException invalidCommand() {
        return invalidCommand("Rcon 命令不能为空");
    }

    public static RconException invalidCommand(String message) {
        return new RconException(message, null, Kind.INVALID_COMMAND);
    }

    public static RconException commandFailed(Throwable cause) {
        return new RconException("Rcon 命令执行失败", cause, Kind.COMMAND_FAILED);
    }
}
