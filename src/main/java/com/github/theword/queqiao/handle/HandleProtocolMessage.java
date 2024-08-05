package com.github.theword.queqiao.handle;

import com.github.theword.queqiao.constant.BaseConstant;
import com.github.theword.queqiao.payload.ActionbarPayload;
import com.github.theword.queqiao.payload.BasePayload;
import com.github.theword.queqiao.payload.MessagePayload;
import com.github.theword.queqiao.payload.SendTitlePayload;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.java_websocket.WebSocket;

import static com.github.theword.queqiao.utils.Tool.*;

public class HandleProtocolMessage {

    /**
     * 处理websocket消息
     *
     * @param webSocket WebSocket
     * @param message   websocket消息
     */
    public void handleWebSocketJson(WebSocket webSocket, String message) {
        // 组合消息
        if (config.isDebug())
            logger.debug("收到来自 {} 的 WebSocket 消息：{}", webSocket.getRemoteSocketAddress(), message);
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
            case "command":
                // TODO Support command
            default:
                logger.warn(BaseConstant.UNKNOWN_API + "{}", basePayload.getApi());
                break;
        }
    }
}
