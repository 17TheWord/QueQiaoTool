package com.github.theword.queqiao.tool.event.base


open class BaseNoticeEvent(
    eventName: String, subType: String, private val player: BasePlayer
) : BaseEvent(eventName, "notice", subType)
