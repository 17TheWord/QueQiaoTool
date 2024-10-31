package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageSegment {
    private String type;
    private CommonTextComponent data;
}
