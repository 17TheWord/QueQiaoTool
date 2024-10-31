package com.github.theword.queqiao.tool.event.base

class BasePlayerChatEvent(
    eventName: String, messageId: String, player: BasePlayer, message: String
) : BaseMessageEvent(eventName, "chat", messageId, player, message)
