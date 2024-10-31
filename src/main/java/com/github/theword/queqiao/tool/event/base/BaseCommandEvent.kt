package com.github.theword.queqiao.tool.event.base

class BaseCommandEvent(
    eventName: String, messageId: String, player: BasePlayer, command: String
) : BaseMessageEvent(eventName, "player_command", messageId, player, command)
