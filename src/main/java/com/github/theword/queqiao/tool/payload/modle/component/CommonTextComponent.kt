package com.github.theword.queqiao.tool.payload.modle.component

import com.github.theword.queqiao.tool.payload.modle.click.CommonClickEvent
import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverEvent
import com.google.gson.annotations.SerializedName

class CommonTextComponent(
    text: String,
    color: String? = null,
    font: String? = null,
    bold: Boolean = false,
    italic: Boolean = false,
    underlined: Boolean = false,
    strikethrough: Boolean = false,
    obfuscated: Boolean = false,
    insertion: String? = null,
    @field:SerializedName("click_event") val clickEvent: CommonClickEvent? = null,
    @field:SerializedName("hover_event") val hoverEvent: CommonHoverEvent? = null
) : CommonBaseComponent(text, color, font, bold, italic, underlined, strikethrough, obfuscated, insertion) {
    constructor(text: String) : this(text, null, null, false, false, false, false, false, null)
}
