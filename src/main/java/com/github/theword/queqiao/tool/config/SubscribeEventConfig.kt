package com.github.theword.queqiao.tool.config


data class SubscribeEventConfig(
    var playerChat: Boolean = true,
    var playerDeath: Boolean = true,
    var playerJoin: Boolean = true,
    var playerQuit: Boolean = true,
    var playerCommand: Boolean = true
)