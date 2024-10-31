package com.github.theword.queqiao.tool.event.base

import com.google.gson.Gson
import java.util.UUID

data class BasePlayer(private val nickname: String, private val uuid: UUID? = null) {

    override fun equals(other: Any?): Boolean {
        if (other !is BasePlayer) return false
        if (this === other) return true
        if (uuid != null && uuid == other.uuid) return true
        return nickname == other.nickname
    }

    override fun hashCode(): Int {
        return nickname.hashCode()
    }

    val json: String
        get() = Gson().toJson(this)
}
