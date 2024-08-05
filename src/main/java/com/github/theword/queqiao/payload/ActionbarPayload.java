package com.github.theword.queqiao.payload;

import com.github.theword.queqiao.payload.modle.CommonBaseComponent;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class ActionbarPayload {
    @SerializedName("message_list")
    private List<CommonBaseComponent> messageList;

    @Override
    public String toString() {
        return messageList.stream()
                .map(CommonBaseComponent::getText)
                .collect(Collectors.joining());
    }
}
