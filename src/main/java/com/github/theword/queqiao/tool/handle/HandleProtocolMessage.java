package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.payload.*;
import com.google.gson.Gson;
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
        Gson gson = new Gson();
        BasePayload basePayload = gson.fromJson(message, BasePayload.class);
        JsonElement data = basePayload.getData();
        switch (basePayload.getApi()) {
            case "broadcast":
            case "send_msg":
                MessagePayload messageList = gson.fromJson(data, MessagePayload.class);
                handleApi.handleBroadcastMessage(webSocket, messageList.getMessageList());
                break;
            case "send_title":
                SendTitlePayload sendTitlePayload = gson.fromJson(data, SendTitlePayload.class);
                handleApi.handleSendTitleMessage(webSocket, sendTitlePayload.getCommonSendTitle());
                break;
            case "actionbar":
                ActionbarPayload actionMessageList = gson.fromJson(data, ActionbarPayload.class);
                handleApi.handleActionBarMessage(webSocket, actionMessageList.getMessageList());
                break;
            case "send_private_msg":
                PrivateMessagePayload privateUUIDMessagePayload = gson.fromJson(data, PrivateMessagePayload.class);
                handleApi.handlePrivateMessage(webSocket, privateUUIDMessagePayload.getTargetPlayerName(), privateUUIDMessagePayload.getTargetPlayerUuid(), privateUUIDMessagePayload.getMessageList());
                return;
            case "command":
                // TODO Support command
            default:
                logger.warn(BaseConstant.UNKNOWN_API + "{}", basePayload.getApi());
                break;
        }
    }
}
