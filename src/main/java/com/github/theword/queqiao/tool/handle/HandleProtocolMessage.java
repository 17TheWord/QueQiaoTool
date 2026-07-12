package com.github.theword.queqiao.tool.handle;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

import com.github.theword.queqiao.tool.constant.CommonConstants;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.protocol.ProtocolRouter;
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
    private final Logger logger;
    private final ProtocolRouter protocolRouter;

    public HandleProtocolMessage(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
        this.protocolRouter = new ProtocolRouter();
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
        Response response = this.handle(rawJsonMessage, CommonConstants.Text.EMPTY, MessageSource.HTTP);
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
            return Response.failed(ProtocolConstants.Status.INTERNAL_ERROR, ProtocolConstants.Message.PARSE_MESSAGE_FAILED, data, null);
        }
    }

    /**
     * 业务处理核心：接收原始 JSON 字符串并解析与处理
     *
     * @return Response 处理结果
     */
    public Response parseAndHandle(BasePayload basePayload) {
        return protocolRouter.route(basePayload);
    }
}
