package com.github.theword.queqiao.tool.payload.modle.component;

import com.github.theword.queqiao.tool.payload.modle.click.CommonClickEvent;
import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverEvent;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class CommonTextComponent {
    private String text;
    private String color;
    private String font;
    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;
    private boolean obfuscated;
    private String insertion;

    @SerializedName("click_event")
    private CommonClickEvent clickEvent;

    @SerializedName("hover_event")
    private CommonHoverEvent hoverEvent;

    @Override
    public String toString() {
        return this.text;
    }
}
