package com.github.theword.queqiao.tool.payload;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.UUID;

/**
 * 私聊消息负载（PrivateMessagePayload）
 *
 * <p>扩展自 {@link MessagePayload}，用于表示发送给单个目标玩家的私聊消息。
 *
 * <p>包含目标玩家的 UUID 和昵称信息，用于在日志或展示中标识接收者。
 */
public class PrivateMessagePayload extends MessagePayload {

    /** 目标玩家的 UUID（可为空） */
    @SerializedName("uuid")
    private UUID uuid = null;

    /** 目标玩家的昵称（可为空） */
    @SerializedName("nickname")
    private String nickname = null;

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public PrivateMessagePayload() {
    }

    public PrivateMessagePayload(List<MessageSegment> message) {
        super(message);
    }

    public PrivateMessagePayload(UUID uuid, String nickname) {
        this.uuid = uuid;
        this.nickname = nickname;
    }

    public PrivateMessagePayload(List<MessageSegment> message, UUID uuid, String nickname) {
        super(message);
        this.uuid = uuid;
        this.nickname = nickname;
    }

    /**
     * 将私聊消息转换为描述字符串，用于日志输出。
     *
     * <p>输出格式示例："send private message to @nickname:uuid: message"。
     *
     * <p>当 nickname 或 uuid 为空时，会根据可用信息回退为合适的字符串（例如仅 uuid、仅昵称或 Unknown player）。
     *
     * @return 描述目标与消息内容的字符串
     */
    @Override
    public String toString() {
        String tempTargetPlayerName;

        if (nickname != null && !nickname.isEmpty()) {
            if (uuid != null) {
                tempTargetPlayerName = String.format("@%s:%s", nickname, uuid);
            } else {
                tempTargetPlayerName = nickname;
            }
        } else if (uuid != null) {
            tempTargetPlayerName = uuid.toString();
        } else {
            tempTargetPlayerName = "Unknown player";
        }

        return String.format("send private message to %s: %s", tempTargetPlayerName, super.toString());
    }
}
