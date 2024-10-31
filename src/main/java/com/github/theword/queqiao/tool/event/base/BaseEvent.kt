package com.github.theword.queqiao.tool.event.base

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * BaseEvent
 *
 *
 * serverName 将在发送前通过配置文件获取并填充
 */
open class BaseEvent(
    @field:SerializedName("event_name") private val eventName: String,
    @field:SerializedName("post_type") private val postType: String,
    @field:SerializedName("sub_type") private val subType: String
) {
    private val timestamp = (System.currentTimeMillis() / 1000).toInt()

    @SerializedName("server_name")
    var serverName: String = ""

    val json: String
        get() {
            val gson = Gson()
            return gson.toJson(this)
        }
}
