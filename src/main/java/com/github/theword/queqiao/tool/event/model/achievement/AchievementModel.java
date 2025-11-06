package com.github.theword.queqiao.tool.event.model.achievement;

/**
 * 成就模型
 */
public class AchievementModel {
    private DisplayModel display;

    public DisplayModel getDisplay() {
        return display;
    }

    public void setDisplay(DisplayModel value) {
        this.display = value;
    }

    public AchievementModel() {
    }

    public AchievementModel(DisplayModel displayModel) {
        this.display = displayModel;
    }
}