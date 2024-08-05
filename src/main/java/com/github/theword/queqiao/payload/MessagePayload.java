package com.github.theword.queqiao.payload;

import com.github.theword.queqiao.payload.modle.CommonBaseComponent;
import com.github.theword.queqiao.payload.modle.CommonTextComponent;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MessagePayload {

    @SerializedName("message_list")
    private List<CommonTextComponent> messageList;

    @Override
    public String toString() {
        return messageList.stream()
                .map(CommonBaseComponent::getText)
                .collect(Collectors.joining());
    }
}
