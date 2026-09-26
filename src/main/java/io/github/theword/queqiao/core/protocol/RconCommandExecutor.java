package io.github.theword.queqiao.core.protocol;

import io.github.theword.queqiao.core.exception.rcon.RconException;

/**
 * RCON 命令执行器
 *
 * <p>协议层<b>不直接持有</b> Rcon 客户端，而是通过该函数式接口请求执行——
 * 这样协议层既不依赖全局状态，也不依赖 Rcon 的实现细节。
 *
 * <p>实现方为 {@code QueQiaoRuntime}（内部管道），<b>平台实现无需关心</b>。
 *
 * @since 0.6.11
 */
@FunctionalInterface
public interface RconCommandExecutor {

    /**
     * 执行一条 RCON 命令
     *
     * @param command 命令内容
     * @return 命令执行结果
     * @throws RconException 未启用、未连接或执行失败
     */
    String execute(String command) throws RconException;
}
