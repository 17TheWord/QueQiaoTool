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
    private int fadeIn = 20;

    /**
     * 停留时间（ticks），默认 70
     */
    private int stay = 70;

    /**
     * 淡出时间（ticks），默认 10
     */
    private int fadeOut = 20;


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

    public int getFadeIn() {
        return fadeIn;
    }

    public void setFadeIn(int fadeIn) {
        this.fadeIn = fadeIn;
    }

    public int getStay() {
        return stay;
    }

    public void setStay(int stay) {
        this.stay = stay;
    }

    public int getFadeOut() {
        return fadeOut;
    }

    public void setFadeOut(int fadeOut) {
        this.fadeOut = fadeOut;
    }

    public TitlePayload() {
    }

    public TitlePayload(JsonElement title, JsonElement subtitle, int fadeIn, int stay, int fadeOut) {
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
    }
}
