package com.github.theword.queqiao.tool.payload.modle.component;

import com.github.theword.queqiao.tool.payload.modle.click.CommonClickEvent;
import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverEvent;
import com.google.gson.annotations.SerializedName;

public class CommonTextComponent extends CommonBaseComponent {


    @SerializedName("click_event")
    private CommonClickEvent clickEvent;

    @SerializedName("hover_event")
    private CommonHoverEvent hoverEvent;

    public CommonClickEvent getClickEvent() {
        return clickEvent;
    }

    public void setClickEvent(CommonClickEvent clickEvent) {
        this.clickEvent = clickEvent;
    }

    public CommonHoverEvent getHoverEvent() {
        return hoverEvent;
    }

    public void setHoverEvent(CommonHoverEvent hoverEvent) {
        this.hoverEvent = hoverEvent;
    }

    public CommonTextComponent() {
    }

    public CommonTextComponent(String text) {
        super(text);
    }

    public CommonTextComponent(String text, String color, String font, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean obfuscated, String insertion) {
        super(text, color, font, bold, italic, underlined, strikethrough, obfuscated, insertion);
    }

    public CommonTextComponent(String text, CommonClickEvent clickEvent, CommonHoverEvent hoverEvent) {
        super(text);
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
    }

    public CommonTextComponent(CommonClickEvent clickEvent, CommonHoverEvent hoverEvent) {
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
    }

    public CommonTextComponent(String text, String color, String font, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean obfuscated, String insertion, CommonClickEvent clickEvent, CommonHoverEvent hoverEvent) {
        super(text, color, font, bold, italic, underlined, strikethrough, obfuscated, insertion);
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
