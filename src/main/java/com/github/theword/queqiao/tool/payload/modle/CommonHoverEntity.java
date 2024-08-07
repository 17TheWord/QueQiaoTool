package com.github.theword.queqiao.tool.payload.modle;

import lombok.Data;

import java.util.List;

@Data
public class CommonHoverEntity {
    String type;
    String id;
    List<? extends CommonBaseComponent> name;
}
