package com.github.theword.queqiao.tool.utils

import com.github.theword.queqiao.tool.payload.MessageSegment
import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken

object PayloadUtils {
    /**
     * 解析消息段列表
     *
     * 消息可能为字符串、MessageSegment对象，MessageSegment列表
     *
     * @param element JsonElement
     * @param context JsonDeserializationContext
     * @return List<MessageSegment>
    </MessageSegment> */
    @JvmStatic
    fun deserializeMessageSegmentList(element: JsonElement, context: JsonDeserializationContext): List<MessageSegment> {
        if (element.isJsonArray) {
            return context.deserialize(element, object : TypeToken<List<MessageSegment?>?>() {
            }.type)
        } else if (element.isJsonPrimitive) {
            val text = element.asString
            val segment = MessageSegment("text", CommonTextComponent(text))
            return listOf(segment)
        } else if (element.isJsonObject) {
            return listOf(context.deserialize(element.asJsonObject, MessageSegment::class.java))
        } else {
            val messageSegment = MessageSegment("text", CommonTextComponent("Unknown Message"))
            return listOf(messageSegment)
        }
    }
}
