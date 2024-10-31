package com.github.theword.queqiao.tool.event.base

class BasePlayerQuitEvent(
    eventName: String, player: BasePlayer
) : BaseNoticeEvent(eventName, "quit", player)
