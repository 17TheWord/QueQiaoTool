package com.github.theword.queqiao.payload.modle;


import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class CommonHoverEvent {
    private String action;
    @SerializedName("base_component_list")
    private List<CommonBaseComponent> baseComponentList;
    private CommonHoverItem item;
    private CommonHoverEntity entity;
}
