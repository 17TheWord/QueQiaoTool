package com.github.theword.queqiao.tool.payload.modle.component


open class CommonBaseComponent(
    var text: String,
    var color: String? = null,
    var font: String? = null,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underlined: Boolean = false,
    var strikethrough: Boolean = false,
    var obfuscated: Boolean = false,
    var insertion: String? = null
) {
    override fun toString(): String {
        return text
    }
}
