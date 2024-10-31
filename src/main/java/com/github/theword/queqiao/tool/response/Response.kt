package com.github.theword.queqiao.tool.response

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Response
 *
 * code：返回值
 *
 * message：返回信息
 *
 * data：数据
 *
 * echo：请求的echo，从请求中获取
 */
data class Response(
    var code: Int,
    var status: ResponseEnum,
    var message: String,
    var data: Any?,
    var echo: String?
) {
    @SerializedName("post_type")
    private val postType = "response"

    val json: String
        get() {
            val gson = Gson()
            return gson.toJson(this)
        }
}
