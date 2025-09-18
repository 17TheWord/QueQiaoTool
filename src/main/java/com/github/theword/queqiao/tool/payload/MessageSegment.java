package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息段（Message Segment）
 * <p>表示消息的一个组成部分，包含类型和对应的数据对象。</p>
 * <p>type：消息段类型（例如 "text"、"image" 等）。</p>
 * <p>data：与类型对应的数据载体，在当前实现中通常为 {@link com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent}。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSegment {
    /**
     * 消息段类型
     */
    private String type;

    /**
     * 消息段数据对象
     */
    private CommonTextComponent data;
}
