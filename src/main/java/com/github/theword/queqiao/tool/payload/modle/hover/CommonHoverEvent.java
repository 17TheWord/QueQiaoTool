package com.github.theword.queqiao.tool.payload.modle.hover;


import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import lombok.Data;

import java.util.List;

@Data
public class CommonHoverEvent {
    private String action;
    private List<CommonTextComponent> text;
    private CommonHoverItem item;
    private CommonHoverEntity entity;
}
