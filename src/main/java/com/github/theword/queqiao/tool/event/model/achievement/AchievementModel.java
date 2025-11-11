package com.github.theword.queqiao.tool.event.model.achievement;

/**
 * 成就模型
 */
public class AchievementModel {

    private String key;

    private DisplayModel display;

    private String text;

    /**
     * 格式化成就消息
     * <p> frame 不能为空，需先对其进行 set
     *
     * @param nickname 玩家昵称
     * @param title    成就标题
     * @return 格式化后的成就消息
     */
    public String pattern(String frame, String nickname, String title) {
        String result;
        switch (frame.toLowerCase()) {
            case "goal":
                result = " has reached the goal ";
                break;
            case "challenge":
                result = " has completed the challenge ";
                break;
            case "task":
                result = " has completed the task ";
                break;
            default:
                result = " has made the advancement ";
                break;
        }
        text = nickname + result + title;
        return text;
    }

    public AchievementModel() {
    }

    public AchievementModel(String key, DisplayModel display, String text) {
        this.key = key;
        this.display = display;
        this.text = text;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DisplayModel getDisplay() {
        return display;
    }

    public void setDisplay(DisplayModel display) {
        this.display = display;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}