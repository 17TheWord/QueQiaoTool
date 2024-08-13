package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.modle.CommonBaseComponent;
import com.github.theword.queqiao.tool.payload.modle.CommonSendTitle;
import com.github.theword.queqiao.tool.payload.modle.CommonTextComponent;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.UUID;

public interface HandleApi {

    /**
     * 广播消息
     *
     * @param webSocket   WebSocket
     * @param messageList 消息列表
     */
    void handleBroadcastMessage(WebSocket webSocket, List<CommonTextComponent> messageList);

    /**
     * 广播 Send Title 消息
     *
     * @param webSocket       WebSocket
     * @param commonSendTitle Send Title
     */
    void handleSendTitleMessage(WebSocket webSocket, CommonSendTitle commonSendTitle);

    /**
     * 广播 Action Bar 消息
     *
     * @param webSocket   WebSocket
     * @param messageList Action Bar 消息列表
     */
    void handleActionBarMessage(WebSocket webSocket, List<CommonBaseComponent> messageList);

    /**
     * 发送私聊消息
     *
     * @param webSocket        WebSocket
     * @param targetPlayerName 目标玩家名
     * @param targetPlayerUuid 目标 UUID
     * @param messageList      消息列表
     */
    void handlePrivateMessage(WebSocket webSocket, String targetPlayerName, UUID targetPlayerUuid, List<CommonTextComponent> messageList);
}
