package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class TitlePayload {

    private List<CommonTextComponent> title;

    private List<CommonTextComponent> subtitle;

    private int fadein;

    private int stay;

    private int fadeout;

    public String toTitleString() {
        return title.stream()
                .map(CommonTextComponent::getText)
                .collect(Collectors.joining());
    }

    public String toSubtitleString() {
        return subtitle.stream()
                .map(CommonTextComponent::getText)
                .collect(Collectors.joining());
    }

}
