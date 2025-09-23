package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息负载类（MessagePayload）
 * <p>表示一条消息的负载，包含多个消息段（MessageSegment）。</p>
 * <p>在系统中用于承载需要发送或接收的消息内容，并在需要时转换为纯文本表示。</p>
 */
public class MessagePayload {

    /**
     * 消息段列表
     * <p>每个元素为一个消息段，按照顺序组成完整消息。</p>
     */
    @SerializedName("message")
    private List<MessageSegment> message;

    public List<MessageSegment> getMessage() {
        return message;
    }

    public void setMessage(List<MessageSegment> message) {
        this.message = message;
    }

    public MessagePayload() {
    }

    public MessagePayload(List<MessageSegment> message) {
        this.message = message;
    }

    /**
     * 将消息段列表转换为纯文本字符串输出，用于日志或简单展示。
     *
     * @return 由所有消息段文本拼接而成的字符串
     */
    @Override
    public String toString() {
        return message.stream()
                .map(MessageSegment::getData)
                .map(CommonTextComponent::getText)
                .collect(Collectors.joining());
    }
}
