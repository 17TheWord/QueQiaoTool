package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.ApiConstants;
import com.github.theword.queqiao.tool.exception.rcon.RconCommandException;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.CommandPayload;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.util.HashMap;

/**
 * 处理 Rcon 命令 API。
 */
public class SendRconCommandHandler implements ProtocolHandler {
    private final Gson gson;
    private final Logger logger;

    public SendRconCommandHandler(Gson gson, Logger logger) {
        this.gson = gson;
        this.logger = logger;
    }

    @Override
    public Response handle(BasePayload payload) {
        CommandPayload commandPayload = gson.fromJson(payload.getData(), CommandPayload.class);
        String result;
        try {
            result = GlobalContext.sendRconCommand(commandPayload.getCommand());
            logger.info("发送 Rcon 命令: {}", commandPayload.getCommand());
            return Response.success(result);
        } catch (RconCommandException e) {
            return handleRconCommandException(commandPayload.getCommand(), e);
        }
    }

    private Response handleRconCommandException(String command, RconCommandException e) {
        switch (e.getReason()) {
            case DISABLED:
                logger.warn("Rcon 命令发送失败，Rcon 未启用。command={}", command);
                return failed(command, ApiConstants.Code.BAD_REQUEST, e.getMessage());
            case NOT_CONNECTED:
                logger.warn("Rcon 命令发送失败，Rcon 未连接。command={}", command);
                return failed(command, ApiConstants.Code.BAD_REQUEST, e.getMessage());
            case FAILED:
            default:
                logger.warn("Rcon 执行命令时出现问题，命令发送失败！command={}, error={}", command, e.getMessage(), e);
                return failed(command, ApiConstants.Code.INTERNAL_ERROR, e.getMessage());
        }
    }

    private Response failed(String command, int code, String errorMessage) {
        HashMap<Object, Object> resultData = new HashMap<>();
        resultData.put("command", command);
        resultData.put("error", errorMessage);
        return Response.failed(code, errorMessage, resultData);
    }
}
