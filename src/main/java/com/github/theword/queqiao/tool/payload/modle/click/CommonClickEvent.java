package com.github.theword.queqiao.tool.payload.modle.click;

public class CommonClickEvent {
    private String action;
    private String value;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CommonClickEvent() {
    }

    public CommonClickEvent(String action, String value) {
        this.action = action;
        this.value = value;
    }
}
