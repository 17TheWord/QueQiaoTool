package com.github.theword.queqiao.tool.handle;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.payload.*;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.HashMap;

/**
 * 处理协议消息
 */
public class HandleProtocolMessage {

    private final Gson gson;
    private static final HandleApiService handleApiService = GlobalContext.getHandleApiService();
    private final Logger logger;

    public HandleProtocolMessage(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
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
            return this.parseAndHandle(rawJsonMessage);
        } catch (Exception e) {
            this.logger.error("解析来自 {} 的 webSocket 消息时出现问题，消息内容： {}", address, rawJsonMessage);
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
    public Response parseAndHandle(String rawJsonMessage) {
        BasePayload basePayload = gson.fromJson(rawJsonMessage, BasePayload.class);

        String api = basePayload.getApi();
        JsonElement data = basePayload.getData();
        String echo = basePayload.getEcho();

        switch (api) {
            case "broadcast":
            case "send_msg": {
                MessagePayload messagePayload = gson.fromJson(data, MessagePayload.class);
                handleApiService.handleBroadcastMessage(messagePayload.getMessage());
                return Response.success(null, echo);
            }
            case "send_title": {
                TitlePayload titlePayload = gson.fromJson(data, TitlePayload.class);
                if ((titlePayload.getTitle() == null || titlePayload.getTitle().isJsonNull()) && (titlePayload.getSubtitle() == null || titlePayload.getSubtitle().isJsonNull())) {
                    return Response.failed(400, "Title and Subtitle cannot both be null", null, echo);
                }
                handleApiService.handleSendTitleMessage(titlePayload.getTitle(), titlePayload.getSubtitle(), titlePayload.getFadeIn(), titlePayload.getStay(), titlePayload.getFadeOut());
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
                    return Response.failed(400, PrivateMessageResponse.playerIsNull().getMessage(), PrivateMessageResponse.playerIsNull(), echo);
                }
                PrivateMessageResponse privateMessageResponse = handleApiService.handleSendPrivateMessage(privateMessagePayload.getNickname(), privateMessagePayload.getUuid(), privateMessagePayload.getMessage());
                return Response.success(privateMessageResponse, echo);
            }
            case "send_command":
                return Response.failed(500, api + " is not supported now", null, echo);
            case "send_rcon_command":
                CommandPayload commandPayload = gson.fromJson(data, CommandPayload.class);
                String result;
                try {
                    result = GlobalContext.sendRconCommand(commandPayload.getCommand());
                    logger.info("发送 Rcon 命令: {}", commandPayload.getCommand());
                    return Response.success(result, echo);
                } catch (Exception e) {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "failed";
                    logger.warn("Rcon 执行命令时出现问题，命令发送失败！{}", errorMessage);
                    HashMap<Object, Object> resultData = new HashMap<>();
                    resultData.put("command", commandPayload.getCommand());
                    resultData.put("error", errorMessage);
                    return Response.failed(400, errorMessage, resultData, echo);
                }
            default:
                this.logger.warn(BaseConstant.UNKNOWN_API + "{}", api);
                return Response.failed(404, BaseConstant.UNKNOWN_API + api, null, echo);
        }
    }
}
