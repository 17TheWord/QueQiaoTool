package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.google.gson.annotations.SerializedName;

public class PrivateMessageResponse {
    @SerializedName("target_player")
    private PlayerModel player;

    private String message;

    public PrivateMessageResponse() {
    }

    public PrivateMessageResponse(PlayerModel playerModel, String message) {
        this.player = playerModel;
        this.message = message;
    }

    public PlayerModel getPlayer() {
        return player;
    }

    public void setPlayer(PlayerModel playerModel) {
        this.player = playerModel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static PrivateMessageResponse of(PlayerModel playerModel, String message) {
        return new PrivateMessageResponse(playerModel, message);
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

    public static PrivateMessageResponse sendSuccess(PlayerModel playerModel) {
        return of(playerModel, "Send private message success.");
    }
}
