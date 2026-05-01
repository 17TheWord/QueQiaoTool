package com.github.theword.queqiao.tool.handle;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.handle.protocol.BroadcastHandler;
import com.github.theword.queqiao.tool.handle.protocol.GetStatusHandler;
import com.github.theword.queqiao.tool.handle.protocol.ProtocolHandler;
import com.github.theword.queqiao.tool.handle.protocol.ProtocolHandlerRegistry;
import com.github.theword.queqiao.tool.handle.protocol.SendActionBarHandler;
import com.github.theword.queqiao.tool.handle.protocol.SendCommandHandler;
import com.github.theword.queqiao.tool.handle.protocol.SendPrivateMessageHandler;
import com.github.theword.queqiao.tool.handle.protocol.SendRconCommandHandler;
import com.github.theword.queqiao.tool.handle.protocol.SendTitleHandler;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.HashMap;

/**
 * 处理协议消息
 */
public class HandleProtocolMessage {

    private final Gson gson;
    private final ProtocolHandlerRegistry handlerRegistry;
    private final Logger logger;

    public HandleProtocolMessage(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
        this.handlerRegistry = createDefaultHandlerRegistry(gson, logger, GlobalContext.getHandleApiService());
    }

    public HandleProtocolMessage(Logger logger, Gson gson, ProtocolHandlerRegistry handlerRegistry) {
        this.logger = logger;
        this.gson = gson;
        this.handlerRegistry = handlerRegistry;
    }

    private static ProtocolHandlerRegistry createDefaultHandlerRegistry(Gson gson, Logger logger, HandleApiService handleApiService) {
        return new ProtocolHandlerRegistry()
                .register("broadcast", new BroadcastHandler(gson, handleApiService))
                .register("send_msg", new BroadcastHandler(gson, handleApiService))
                .register("send_title", new SendTitleHandler(gson, handleApiService))
                .register("send_actionbar", new SendActionBarHandler(gson, handleApiService))
                .register("send_private_msg", new SendPrivateMessageHandler(gson, handleApiService))
                .register("send_command", new SendCommandHandler())
                .register("send_rcon_command", new SendRconCommandHandler(gson, logger))
                .register("get_status", new GetStatusHandler(logger));
    }

    /**
     * 消息来源枚举
     */
    private enum MessageSource {
        HTTP, WEBSOCKET;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * WebSocket入口，处理JSON字符串
     *
     * @param webSocket      WebSocket连接
     * @param rawJsonMessage 收到的JSON字符串
     * @return 响应的JSON字符串
     */
    public String handleWebsocketJson(WebSocket webSocket, String rawJsonMessage) {
        Response response = this.handle(rawJsonMessage, webSocket.getRemoteSocketAddress().toString(), MessageSource.WEBSOCKET);
        return gson.toJson(response);
    }

    /**
     * Http，处理JSON字符串
     *
     * @param rawJsonMessage 收到的JSON字符串
     * @return 响应的JSON字符串
     */
    public String handleHttpJson(String rawJsonMessage) {
        Response response = this.handle(rawJsonMessage, "", MessageSource.HTTP);
        return gson.toJson(response);
    }

    private Response handle(String rawJsonMessage, String address, MessageSource source) {
        debugLog("收到来自 {} 的 {} 消息：{}", address, source.toString(), rawJsonMessage);
        try {
            BasePayload basePayload = gson.fromJson(rawJsonMessage, BasePayload.class);
            Response response = this.parseAndHandle(basePayload);
            response.setApi(basePayload.getApi());
            response.setEcho(basePayload.getEcho());
            return response;
        } catch (Exception e) {
            this.logger.error("解析来自 {} 的 webSocket 消息时出现问题，消息内容：{}", address, rawJsonMessage);
            this.logger.error("错误信息：", e);
            HashMap<String, String> data = new HashMap<>();
            data.put("rawJsonMessage", rawJsonMessage);
            return Response.failed(500, "解析消息失败", data, null);
        }
    }

    /**
     * 业务处理核心：接收原始 JSON 字符串并解析与处理
     *
     * @return Response 处理结果
     */
    public Response parseAndHandle(BasePayload basePayload) {
        String api = basePayload.getApi();
        ProtocolHandler handler = handlerRegistry.get(api);
        if (handler == null) {
            this.logger.warn(BaseConstant.UNKNOWN_API + "{}", api);
            return Response.failed(404, BaseConstant.UNKNOWN_API + api);
        }
        return handler.handle(basePayload);
    }
}
