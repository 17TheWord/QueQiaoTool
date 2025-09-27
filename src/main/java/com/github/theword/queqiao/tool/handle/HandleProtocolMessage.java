package com.github.theword.queqiao.tool.handle;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.payload.*;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.response.Response;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * 处理协议消息
 */
public class HandleProtocolMessage {

    private static final Gson gson = GsonUtils.buildGson();
    private static final HandleApiService handleApiService = GlobalContext.getHandleApiService();
    private final Logger logger;

    public HandleProtocolMessage(Logger logger) {
        this.logger = logger;
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
        Response response = this.handle(
                rawJsonMessage, webSocket.getRemoteSocketAddress().toString(), MessageSource.WEBSOCKET);
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
            return this.parseAndHandle(rawJsonMessage);
        } catch (Exception e) {
            this.logger.error("解析来自 {} 的 webSocket 消息时出现问题，消息内容： {}", address, rawJsonMessage);
            this.logger.error("错误信息：", e);
            return Response.failed(500, "解析消息失败", null, null);
        }
    }

    /**
     * 业务处理核心：接收原始 JSON 字符串并解析与处理
     *
     * @return Response 处理结果
     */
    public Response parseAndHandle(String rawJsonMessage) {
        BasePayload basePayload = gson.fromJson(rawJsonMessage, BasePayload.class);

        JsonElement data = basePayload.getData();
        String echo = basePayload.getEcho();

        String api = basePayload.getApi();
        switch (api) {
            case "broadcast":
            case "send_msg": {
                MessagePayload messageList = gson.fromJson(data, MessagePayload.class);
                handleApiService.handleBroadcastMessage(messageList.getMessage());
                return Response.success(null, echo);
            }
            case "send_title": {
                TitlePayload titlePayload = gson.fromJson(data, TitlePayload.class);
                handleApiService.handleSendTitleMessage(titlePayload);
                return Response.success(null, echo);
            }
            case "send_actionbar": {
                MessagePayload actionMessagePayload = gson.fromJson(data, MessagePayload.class);
                handleApiService.handleSendActionBarMessage(actionMessagePayload.getMessage());
                return Response.success(null, echo);
            }
            case "send_private_msg": {
                PrivateMessagePayload privateMessagePayload = gson.fromJson(data, PrivateMessagePayload.class);
                if ((privateMessagePayload.getNickname() == null || privateMessagePayload.getNickname().isEmpty()) && privateMessagePayload.getUuid() == null) {
                    return Response.failed(
                            400, PrivateMessageResponse.playerIsNull().getMessage(), PrivateMessageResponse.playerIsNull(), echo);
                }
                PrivateMessageResponse privateMessageResponse = handleApiService.handleSendPrivateMessage(
                        privateMessagePayload.getNickname(), privateMessagePayload.getUuid(), privateMessagePayload.getMessage());
                return Response.success(privateMessageResponse, echo);
            }
            case "send_command":
                return Response.failed(500, api + " is not supported now", null, echo);
            case "send_rcon_command":
                CommandPayload commandPayload = gson.fromJson(data, CommandPayload.class);
                String result;
                try {
                    result = GlobalContext.sendRconCommand(commandPayload.getCommand());
                    return Response.success(result, echo);
                } catch (IllegalArgumentException e) {
                    logger.warn("Rcon 执行命令时出现问题，命令发送失败：{}", e.getMessage());
                    return Response.failed(400, e.getMessage(), null, echo);
                } catch (IOException e) {
                    logger.warn("Rcon 执行命令时出现问题，命令发送失败！", e);
                    return Response.failed(500, "failed", null, echo);
                }
            default:
                this.logger.warn(BaseConstant.UNKNOWN_API + "{}", api);
                return Response.failed(404, BaseConstant.UNKNOWN_API + api, null, echo);
        }
    }
}
