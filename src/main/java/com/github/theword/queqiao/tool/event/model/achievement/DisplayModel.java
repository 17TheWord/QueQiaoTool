package com.github.theword.queqiao.tool.event.model.achievement;

/**
 * 成就显示模型
 */
public class DisplayModel {

    /**
     * 标题，Fabric、Forge、NeoForge、Spigot、Paper
     */
    private String title;

    /**
     * 描述，Fabric、Forge、NeoForge、Spigot、Paper
     */
    private String description;

    /**
     * Fabric、Forge、NeoForge、Paper
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