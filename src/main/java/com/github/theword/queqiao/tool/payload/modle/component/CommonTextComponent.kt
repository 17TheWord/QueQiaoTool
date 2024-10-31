package com.github.theword.queqiao.tool.payload.modle.component;

import com.github.theword.queqiao.tool.payload.modle.click.CommonClickEvent;
import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverEvent;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommonTextComponent extends CommonBaseComponent {


    @SerializedName("click_event")
    private CommonClickEvent clickEvent;

    @SerializedName("hover_event")
    private CommonHoverEvent hoverEvent;

    public CommonTextComponent(String text) {
        super(text);
    }


    @Override
    public String toString() {
        return super.toString();
    }
}
