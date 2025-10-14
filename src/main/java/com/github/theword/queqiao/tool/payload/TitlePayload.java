package com.github.theword.queqiao.tool.payload;

import com.google.gson.JsonElement;


/**
 * 标题负载
 */
public class TitlePayload {
    /**
     * 标题消息段列表
     */
    private JsonElement title;

    /**
     * 副标题消息段列表（可为空）
     */
    private JsonElement subtitle;

    /**
     * 淡入时间（ticks），默认 10
     */
    private int fadein = 10;

    /**
     * 停留时间（ticks），默认 70
     */
    private int stay = 70;

    /**
     * 淡出时间（ticks），默认 10
     */
    private int fadeout = 10;


    public JsonElement getTitle() {
        return title;
    }

    public void setTitle(JsonElement title) {
        this.title = title;
    }

    public JsonElement getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(JsonElement subtitle) {
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

    public TitlePayload(JsonElement title, JsonElement subtitle, int fadein, int stay, int fadeout) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadein = fadein;
        this.stay = stay;
        this.fadeout = fadeout;
    }
}
