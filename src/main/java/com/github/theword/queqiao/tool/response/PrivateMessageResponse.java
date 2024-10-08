package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.event.base.BasePlayer;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageResponse {
    @SerializedName("target_player")
    private BasePlayer player;
    private String message;

    public static PrivateMessageResponse of(BasePlayer player, String message) {
        return new PrivateMessageResponse(player, message);
    }

    public static PrivateMessageResponse playerNotFound() {
        return of(null, "Target player not found.");
    }

    public static PrivateMessageResponse playerNotOnline() {
        return of(null, "Target player is not online.");
    }

    public static PrivateMessageResponse playerIsNull() {
        return of(null, "Target player is null.");
    }

    public static PrivateMessageResponse sendSuccess(BasePlayer player) {
        return of(player, "Send private message success.");
    }
}
