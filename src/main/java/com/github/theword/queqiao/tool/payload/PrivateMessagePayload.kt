package com.github.theword.queqiao.tool.payload;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrivateMessagePayload extends MessagePayload {

    @SerializedName("uuid")
    private UUID uuid = null;

    @SerializedName("nickname")
    private String nickname = null;

    /**
     * 如果目标玩家名称或者uuid为空，则默认为未知玩家
     * <p>否则按照 @nickname:UUID 进行输出</p>
     *
     * @return 发送结果字符串
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
