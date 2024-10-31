package com.github.theword.queqiao.tool.event.base

class BasePlayerDeathEvent(
    eventName: String, messageId: String, player: BasePlayer, message: String
) : BaseMessageEvent(eventName, "death", messageId, player, message)
