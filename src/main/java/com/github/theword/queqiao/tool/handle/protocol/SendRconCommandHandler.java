package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.GlobalContext;
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
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "failed";
            logger.warn("Rcon 执行命令时出现问题，命令发送失败！{}", errorMessage);
            HashMap<Object, Object> resultData = new HashMap<>();
            resultData.put("command", commandPayload.getCommand());
            resultData.put("error", errorMessage);
            return Response.failed(400, errorMessage, resultData);
        }
    }
}
