package com.github.theword.queqiao.tool.payload

import com.google.gson.JsonElement
import lombok.Data

/**
 * BasePayload
 *
 * api：API名称
 *
 * data：Json对象，在 [com.github.theword.queqiao.tool.handle.HandleProtocolMessage] 中处理
 *
 * echo：用于请求结果以及返回值
 */
data class BasePayload(
    val api: String,
    val data: JsonElement,
    val echo: String? = null
)
