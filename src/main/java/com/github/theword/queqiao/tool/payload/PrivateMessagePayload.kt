package com.github.theword.queqiao.tool.payload

import java.util.*

class PrivateMessagePayload : MessagePayload() {
    var nickname: String? = null
    var uuid: UUID? = null

    /**
     * 如果目标玩家名称或者uuid为空，则默认为未知玩家
     *
     * 否则按照 @nickname:UUID 进行输出
     *
     * @return 发送结果字符串
     */
    override fun toString(): String {
        val tempTargetPlayerName = if (!nickname.isNullOrEmpty()) {
            if (uuid != null) {
                String.format("@%s:%s", nickname, uuid)
            } else {
                nickname
            }
        } else uuid?.toString() ?: "Unknown player"

        return String.format("send private message to %s: %s", tempTargetPlayerName, super.toString())
    }
}
