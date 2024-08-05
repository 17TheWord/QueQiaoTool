package com.github.theword.queqiao.payload.modle;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CommonSendTitle {

    private List<CommonBaseComponent> title;

    private List<CommonBaseComponent> subtitle;

    private int fadein;

    private int stay;

    private int fadeout;

    public String toTitleString() {
        return title.stream()
                .map(CommonBaseComponent::getText)
                .collect(Collectors.joining());
    }

    public String toSubtitleString() {
        return subtitle.stream()
                .map(CommonBaseComponent::getText)
                .collect(Collectors.joining());
    }

}
