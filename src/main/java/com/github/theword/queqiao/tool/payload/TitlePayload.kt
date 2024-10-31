package com.github.theword.queqiao.tool.payload

class TitlePayload {
    var title: List<MessageSegment> = listOf()
    var subtitle: List<MessageSegment>? = null
    var fadein: Int = 10
    var stay: Int = 70
    var fadeout: Int = 10
    override fun toString(): String {
        val titleStr = StringBuilder("Title: ${title.joinToString("")}")
        subtitle?.let {
            titleStr.append("\nSubTitle: ${it.joinToString("")}")
        }
        return titleStr.toString()
    }
}
