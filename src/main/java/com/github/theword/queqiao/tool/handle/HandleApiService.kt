package com.github.theword.queqiao.tool.handle

import com.github.theword.queqiao.tool.payload.MessageSegment
import com.github.theword.queqiao.tool.payload.TitlePayload
import com.github.theword.queqiao.tool.response.PrivateMessageResponse
import java.util.UUID

interface HandleApiService {
    /**
     * 广播消息
     *
     * @param messageList 消息列表
     */
    fun handleBroadcastMessage(messageList: List<MessageSegment>)

    /**
     * 广播 Title 消息
     *
     * @param titlePayload Title
     */
    fun handleSendTitleMessage(titlePayload: TitlePayload)

    /**
     * 广播 ActionBar 消息
     *
     * @param messageList Action Bar 消息列表
     */
    fun handleSendActionBarMessage(messageList: List<MessageSegment>)

    /**
     * 发送私聊消息
     *
     * @param nickname    目标玩家名
     * @param uuid        目标 UUID
     * @param messageList 消息列表
     */
    fun handleSendPrivateMessage(
        nickname: String?,
        uuid: UUID?,
        messageList: List<MessageSegment>
    ): PrivateMessageResponse
}
