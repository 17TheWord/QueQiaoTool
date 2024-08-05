package com.github.theword.queqiao.handle;

import com.github.theword.queqiao.payload.modle.CommonBaseComponent;
import com.github.theword.queqiao.payload.modle.CommonTextComponent;
import com.github.theword.queqiao.payload.modle.CommonSendTitle;
import org.java_websocket.WebSocket;

import java.util.List;

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
     * @param webSocket WebSocket
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
}
