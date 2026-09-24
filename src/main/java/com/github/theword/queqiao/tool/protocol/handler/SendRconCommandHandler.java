package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.payload.CommandPayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.response.RconCommandError;
import org.slf4j.Logger;

public class SendRconCommandHandler extends AbstractProtocolHandler<CommandPayload, String> {

    public SendRconCommandHandler(Logger logger) {
        super(logger, CommandPayload.class);
    }

    /**
     * 执行 Rcon 命令
     *
     * <p>状态码语义（不同失败原因不再统一归为 400）：
     * <ul>
     *     <li>命令为空 → 400（调用方请求不合法）</li>
     *     <li>Rcon 未启用 / 未连接 → 503（服务暂不可用）</li>
     *     <li>命令已下发但执行失败 → 500（服务端错误）</li>
     * </ul>
     *
     * <p>日志：不在 INFO 级别记录完整命令内容，因为命令可能包含密码、token 或玩家隐私；
     * 完整内容仅在显式开启 debug 时输出。
     *
     * @param payload 命令负载
     * @return Rcon 执行结果
     * @throws ProtocolException 见上方状态码语义
     */
    @Override
    protected String handlePayload(CommandPayload payload) throws ProtocolException {
        String command = payload.getCommand();
        if (command == null || command.trim().isEmpty()) {
            this.logger.warn("收到空的 Rcon 命令，已拒绝");
            throw ProtocolException.badRequest(ProtocolConstants.Message.RCON_COMMAND_EMPTY);
        }

        try {
            String result = GlobalContext.sendRconCommand(command);
            this.logger.info("已通过 Rcon 执行命令（长度 {}）", command.length());
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Rcon 命令内容（可能含敏感信息，仅 debug 级别输出）：{}", command);
            }
            return result;
        } catch (RconException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : ProtocolConstants.Message.FAILED;
            RconCommandError errorData = new RconCommandError(command, errorMessage);

            if (e.getKind() == RconException.Kind.DISABLED || e.getKind() == RconException.Kind.DISCONNECTED) {
                this.logger.warn("Rcon 暂不可用（{}）：{}", e.getKind(), errorMessage);
                throw ProtocolException.serviceUnavailable(errorMessage, errorData);
            }

            this.logger.warn("Rcon 命令执行失败：{}", errorMessage);
            throw ProtocolException.internalError(errorMessage, errorData);
        }
    }
}
