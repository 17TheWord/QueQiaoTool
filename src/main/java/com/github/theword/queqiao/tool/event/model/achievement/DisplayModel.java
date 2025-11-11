package com.github.theword.queqiao.tool.event.model.achievement;

/**
 * 成就显示模型
 */
public class DisplayModel {

    /**
     * 展示信息标题
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String title;

    /**
     * 展示信息描述
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String description;

    /**
     * 展示信息类型
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String frame;

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getFrame() {
        return frame;
    }

    public void setFrame(String value) {
        this.frame = value.toLowerCase();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

}