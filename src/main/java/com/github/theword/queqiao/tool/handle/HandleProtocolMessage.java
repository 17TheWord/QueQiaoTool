package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.deserializer.MessagePayloadDeserializer;
import com.github.theword.queqiao.tool.deserializer.TitlePayloadDeserializer;
import com.github.theword.queqiao.tool.payload.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.java_websocket.WebSocket;

import static com.github.theword.queqiao.tool.utils.Tool.*;

public class HandleProtocolMessage {

    /**
     * 处理websocket消息
     *
     * @param webSocket WebSocket
     * @param message   websocket消息
     */
    public void handleWebSocketJson(WebSocket webSocket, String message) {
        // 组合消息
        debugLog("收到来自 {} 的 WebSocket 消息：{}", webSocket.getRemoteSocketAddress(), message);
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(MessagePayload.class, new MessagePayloadDeserializer())
                .registerTypeAdapter(TitlePayload.class, new TitlePayloadDeserializer())
                .create();
        BasePayload basePayload = gson.fromJson(message, BasePayload.class);
        JsonElement data = basePayload.getData();
        switch (basePayload.getApi()) {
            case "broadcast":
            case "send_msg":
                MessagePayload messageList = gson.fromJson(data, MessagePayload.class);
                handleApiService.handleBroadcastMessage(webSocket, messageList.getMessage());
                break;
            case "send_title":
                TitlePayload titlePayload = gson.fromJson(data, TitlePayload.class);
                handleApiService.handleSendTitleMessage(webSocket, titlePayload);
                break;
            case "send_actionbar":
                MessagePayload actionMessagePayload = gson.fromJson(data, MessagePayload.class);
                handleApiService.handleActionBarMessage(webSocket, actionMessagePayload.getMessage());
                break;
            case "send_private_msg":
                PrivateMessagePayload privateMessagePayload = gson.fromJson(data, PrivateMessagePayload.class);
                handleApiService.handlePrivateMessage(webSocket, privateMessagePayload.getTargetPlayerName(), privateMessagePayload.getTargetPlayerUuid(), privateMessagePayload.getMessage());
                return;
            case "send_command":
                // TODO Support command
                break;
            default:
                logger.warn(BaseConstant.UNKNOWN_API + "{}", basePayload.getApi());
                break;
        }
    }
}
