package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;

/**
 * 消息段（Message Segment）
 *
 * <p>表示消息的一个组成部分，包含类型和对应的数据对象。
 *
 * <p>type：消息段类型（例如 "text"、"image" 等）。
 *
 * <p>data：与类型对应的数据载体，在当前实现中通常为 {@link
 * com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent}。
 */
public class MessageSegment {
    /** 消息段类型 */
    private String type;

    /** 消息段数据对象 */
    private CommonTextComponent data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CommonTextComponent getData() {
        return data;
    }

    public void setData(CommonTextComponent data) {
        this.data = data;
    }

    public MessageSegment() {
    }

    public MessageSegment(String type, CommonTextComponent data) {
        this.type = type;
        this.data = data;
    }
}
