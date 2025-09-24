package com.github.theword.queqiao.tool.payload.modle.hover;


import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent;

import java.util.List;

public class CommonHoverEvent {
    private String action;
    private List<CommonBaseComponent> text;
    private CommonHoverItem item;
    private CommonHoverEntity entity;

    public CommonHoverEvent() {
    }

    public CommonHoverEvent(String action, List<CommonBaseComponent> text, CommonHoverItem item, CommonHoverEntity entity) {
        this.action = action;
        this.text = text;
        this.item = item;
        this.entity = entity;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<CommonBaseComponent> getText() {
        return text;
    }

    public void setText(List<CommonBaseComponent> text) {
        this.text = text;
    }

    public CommonHoverItem getItem() {
        return item;
    }

    public void setItem(CommonHoverItem item) {
        this.item = item;
    }

    public CommonHoverEntity getEntity() {
        return entity;
    }

    public void setEntity(CommonHoverEntity entity) {
        this.entity = entity;
    }
}
