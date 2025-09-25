package com.github.theword.queqiao.tool.payload;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标题负载（TitlePayload）
 *
 * <p>用于表示游戏内标题（title）与副标题（subtitle）的显示内容及持续时间设置。
 *
 * <p>title 与 subtitle 均为消息段列表，可包含富文本或格式化组件。
 *
 * <p>fadein、stay、fadeout 分别表示淡入、停留和淡出时长，单位为刻（ticks）。
 */
public class TitlePayload {
    /** 标题消息段列表 */
    private List<MessageSegment> title;

    /** 副标题消息段列表（可为空） */
    private List<MessageSegment> subtitle;

    /** 淡入时间（ticks），默认 10 */
    private int fadein = 10;

    /** 停留时间（ticks），默认 70 */
    private int stay = 70;

    /** 淡出时间（ticks），默认 10 */
    private int fadeout = 10;

    public List<MessageSegment> getTitle() {
        return title;
    }

    public void setTitle(List<MessageSegment> title) {
        this.title = title;
    }

    public List<MessageSegment> getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(List<MessageSegment> subtitle) {
        this.subtitle = subtitle;
    }

    public int getFadein() {
        return fadein;
    }

    public void setFadein(int fadein) {
        this.fadein = fadein;
    }

    public int getStay() {
        return stay;
    }

    public void setStay(int stay) {
        this.stay = stay;
    }

    public int getFadeout() {
        return fadeout;
    }

    public void setFadeout(int fadeout) {
        this.fadeout = fadeout;
    }

    public TitlePayload() {
    }

    public TitlePayload(
                        List<MessageSegment> title, List<MessageSegment> subtitle, int fadein, int stay, int fadeout) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadein = fadein;
        this.stay = stay;
        this.fadeout = fadeout;
    }

    /**
     * 将标题负载转换为字符串表示，用于日志或调试输出。
     *
     * @return 包含 title 与 subtitle（如果存在）的可读字符串
     */
    @Override
    public String toString() {
        String titleStr = "Title:" + title.stream().map(MessageSegment::toString).collect(Collectors.joining(""));
        if (subtitle != null)
            titleStr += "\nSubTitle:" + subtitle.stream().map(MessageSegment::toString).collect(Collectors.joining(""));
        return titleStr;
    }
}
