package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MessagePayload {

    @SerializedName("message")
    private List<MessageSegment> message;

    @Override
    public String toString() {
        return message.stream()
                .map(MessageSegment::getData)
                .map(CommonTextComponent::getText)
                .collect(Collectors.joining());
    }
}
