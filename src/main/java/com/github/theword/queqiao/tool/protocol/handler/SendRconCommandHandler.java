package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.payload.CommandPayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.response.RconCommandError;

public class SendRconCommandHandler extends AbstractProtocolHandler<CommandPayload, String> {
    public SendRconCommandHandler() {
        super(CommandPayload.class);
    }

    @Override
    protected String handlePayload(CommandPayload payload) throws ProtocolException {
        try {
            String result = GlobalContext.sendRconCommand(payload.getCommand());
            GlobalContext.getLogger().info("发送 Rcon 命令: {}", payload.getCommand());
            return result;
        } catch (RconException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : ProtocolConstants.Message.FAILED;
            GlobalContext.getLogger().warn("Rcon 执行命令时出现问题，命令发送失败！{}", errorMessage);
            throw ProtocolException.badRequest(errorMessage, new RconCommandError(payload.getCommand(), errorMessage));
        }
    }
}
