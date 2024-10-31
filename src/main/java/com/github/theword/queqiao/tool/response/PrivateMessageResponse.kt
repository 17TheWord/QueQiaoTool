package com.github.theword.queqiao.tool.response

import com.github.theword.queqiao.tool.event.base.BasePlayer
import com.google.gson.annotations.SerializedName

data class PrivateMessageResponse(
    @field:SerializedName("target_player") val player: BasePlayer?,
    val message: String
) {

    companion object {
        fun of(player: BasePlayer?, message: String): PrivateMessageResponse {
            return PrivateMessageResponse(player, message)
        }

        fun playerNotFound(): PrivateMessageResponse {
            return of(null, "Target player not found.")
        }

        fun playerNotOnline(): PrivateMessageResponse {
            return of(null, "Target player is not online.")
        }

        @JvmStatic
        fun playerIsNull(): PrivateMessageResponse {
            return of(null, "Target player is null.")
        }

        fun sendSuccess(player: BasePlayer?): PrivateMessageResponse {
            return of(player, "Send private message success.")
        }
    }
}
