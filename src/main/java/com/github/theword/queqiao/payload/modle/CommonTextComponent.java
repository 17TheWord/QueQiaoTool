package com.github.theword.queqiao.payload.modle;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommonTextComponent extends CommonBaseComponent {

    @SerializedName("click_event")
    private CommonClickEvent clickEvent;

    @SerializedName("hover_event")
    private CommonHoverEvent hoverEvent;

}
