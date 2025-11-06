package com.github.theword.queqiao.tool.event.base;

import com.google.gson.annotations.SerializedName;

/**
 * 通用基础消息事件
 *
 * <p>所有消息类事件的基类
 */
public class BaseMessageEvent extends BaseEvent {

    /**
     * 消息ID
     */
    @SerializedName("message_id")
    private final String messageId;

    /**
     * 原始消息内容
     */
    @SerializedName("raw_message")
    private final String rawMessage;

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param subType   事件子类型
     */
    public BaseMessageEvent(String eventName, String subType, String messageId, String rawMessage) {
        super(eventName, "message", subType);
        this.messageId = messageId;
        this.rawMessage = rawMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getRawMessage() {
        return rawMessage;
    }
}
