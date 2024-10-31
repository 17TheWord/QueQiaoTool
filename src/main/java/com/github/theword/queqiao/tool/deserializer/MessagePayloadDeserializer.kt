package com.github.theword.queqiao.tool.deserializer

import com.github.theword.queqiao.tool.payload.MessagePayload
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload
import com.github.theword.queqiao.tool.utils.PayloadUtils.deserializeMessageSegmentList
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type
import java.util.*

class MessagePayloadDeserializer : JsonDeserializer<MessagePayload> {
    /**
     * 反序列化Message消息
     *
     * 接管所有 MessagePayload 及其子类的反序列化任务
     *
     * @param json    Json数据
     * @param typeOfT 目标类型
     * @param context Json反序列化上下文
     * @return ? extends MessagePayload
     * @throws JsonParseException Json反序列化异常
     */
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessagePayload {
        val jsonObject = json.asJsonObject

        if (jsonObject.has("uuid") || jsonObject.has("nickname")) {
            val privatePayload = PrivateMessagePayload()
            privatePayload.uuid = context.deserialize(jsonObject["uuid"], UUID::class.java)
            privatePayload.nickname = context.deserialize(jsonObject["nickname"], String::class.java)
            privatePayload.message = deserializeMessageSegmentList(jsonObject["message"], context)
            return privatePayload
        } else {
            val payload = MessagePayload()
            val messageElement = jsonObject["message"]
            payload.message = deserializeMessageSegmentList(messageElement, context)
            return payload
        }
    }
}
