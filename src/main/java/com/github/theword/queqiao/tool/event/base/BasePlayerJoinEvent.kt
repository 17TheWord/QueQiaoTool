package com.github.theword.queqiao.tool.event.base

class BasePlayerJoinEvent(
    eventName: String, player: BasePlayer
) : BaseNoticeEvent(eventName, "join", player)
