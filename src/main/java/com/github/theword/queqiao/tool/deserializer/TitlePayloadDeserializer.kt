package com.github.theword.queqiao.tool.deserializer

import com.github.theword.queqiao.tool.payload.TitlePayload
import com.github.theword.queqiao.tool.utils.PayloadUtils.deserializeMessageSegmentList
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class TitlePayloadDeserializer : JsonDeserializer<TitlePayload> {
    /**
     * 反序列化 Title 消息
     *
     * 接管所有 TitlePayload 及其子类的反序列化任务
     *
     * @param json    Json数据
     * @param type    目标类型
     * @param context Json反序列化上下文
     * @return TitlePayload
     * @throws JsonParseException Json反序列化异常
     */
    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): TitlePayload {
        val payload = TitlePayload()
        val jsonObject = json.asJsonObject

        // Deserialize title
        val titleElement = jsonObject["title"]
        if (titleElement != null) {
            payload.title = deserializeMessageSegmentList(titleElement, context)
        }

        // Deserialize subtitle
        val subtitleElement = jsonObject["subtitle"]
        if (subtitleElement != null) {
            payload.subtitle = deserializeMessageSegmentList(subtitleElement, context)
        }

        // Deserialize other fields
        if (jsonObject.has("fadein")) {
            payload.fadein = jsonObject["fadein"].asInt
        }
        if (jsonObject.has("stay")) {
            payload.stay = jsonObject["stay"].asInt
        }
        if (jsonObject.has("fadeout")) {
            payload.fadeout = jsonObject["fadeout"].asInt
        }

        return payload
    }
}
