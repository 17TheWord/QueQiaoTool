package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.CommonBaseComponent;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class PrivateMessagePayload extends MessagePayload {

    @SerializedName("target_player_uuid")
    private UUID targetPlayerUuid = null;

    @SerializedName("target_player_name")
    private String targetPlayerName = null;

    @Override
    public String toString() {
        String tempTargetPlayerName;

        if (targetPlayerName != null && !targetPlayerName.isEmpty())
            tempTargetPlayerName = targetPlayerName;
        else if (targetPlayerUuid != null)
            tempTargetPlayerName = targetPlayerUuid.toString();
        else
            tempTargetPlayerName = "Unknown player";
        return "send private message to " + tempTargetPlayerName + ": " +
                getMessageList().stream()
                        .map(CommonBaseComponent::getText)
                        .collect(Collectors.joining());

    }
}
