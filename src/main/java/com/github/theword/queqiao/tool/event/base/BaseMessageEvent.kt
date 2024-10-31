package com.github.theword.queqiao.tool.event.base

import com.google.gson.annotations.SerializedName

open class BaseMessageEvent(
    eventName: String,
    subType: String,
    @field:SerializedName("message_id") private val messageId: String = "",
    private val player: BasePlayer,
    private val message: String
) : BaseEvent(eventName, "message", subType)
