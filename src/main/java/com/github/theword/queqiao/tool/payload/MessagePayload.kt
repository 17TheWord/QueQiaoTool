package com.github.theword.queqiao.tool.payload

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent
import com.google.gson.annotations.SerializedName
import java.util.stream.Collectors

open class MessagePayload {
    @SerializedName("message")
    var message: List<MessageSegment> = listOf()
    override fun toString(): String {
        return message.stream()
            .map { obj: MessageSegment -> obj.data }
            .map { obj: CommonTextComponent -> obj.text }
            .collect(Collectors.joining())
    }
}
