package com.github.theword.queqiao.tool.payload;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class TitlePayload {
    private List<MessageSegment> title;
    private List<MessageSegment> subtitle;
    private int fadein = 10;
    private int stay = 70;
    private int fadeout = 10;

    @Override
    public String toString() {
        String titleStr = "Title:" + title.stream().map(MessageSegment::toString).collect(Collectors.joining(""));
        if (subtitle != null)
            titleStr += "\nSubTitle:" + subtitle.stream().map(MessageSegment::toString).collect(Collectors.joining(""));
        return titleStr;
    }
}
