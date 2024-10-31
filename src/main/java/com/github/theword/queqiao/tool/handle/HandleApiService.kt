package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;

import java.util.List;
import java.util.UUID;

public interface HandleApiService {

    /**
     * 广播消息
     *
     * @param messageList 消息列表
     */
    void handleBroadcastMessage(List<MessageSegment> messageList);

    /**
     * 广播 Title 消息
     *
     * @param titlePayload Title
     */
    void handleSendTitleMessage(TitlePayload titlePayload);

    /**
     * 广播 ActionBar 消息
     *
     * @param messageList Action Bar 消息列表
     */
    void handleSendActionBarMessage(List<MessageSegment> messageList);

    /**
     * 发送私聊消息
     *
     * @param nickname    目标玩家名
     * @param uuid        目标 UUID
     * @param messageList 消息列表
     */
    PrivateMessageResponse handleSendPrivateMessage(String nickname, UUID uuid, List<MessageSegment> messageList);
}
