package com.github.theword.queqiao.tool.event.model.achievement;

import com.google.gson.annotations.SerializedName;

/**
 * 成就显示模型
 */
public class DisplayModel {
    /**
     * Fabric、Forge、NeoForge、Folia
     */
    @SerializedName("announce_chat")
    private Boolean announceChat;
    /**
     * Fabric、Forge、NeoForge
     */
    private String background;
    /**
     * 描述，Fabric、Forge、NeoForge、Spigot
     */
    private String description;
    /**
     * Fabric、Forge、NeoForge
     */
    private String frame;
    /**
     * Fabric、Forge、NeoForge、Folia
     */
    private Boolean hidden;
    /**
     * 图标，Fabric、Forge、NeoForge、Spigot
     */
    private String icon;
    /**
     * Fabric、Forge、NeoForge、Folia
     */
    @SerializedName("show_toast")
    private Boolean showToast;
    /**
     * 标题，Fabric、Forge、NeoForge、Spigot
     */
    private String title;
    /**
     * Fabric、Forge、NeoForge、Folia
     */
    private Double x;
    /**
     * Fabric、Forge、NeoForge、Folia
     */
    private Double y;

    public Boolean getAnnounceChat() {
        return announceChat;
    }

    public void setAnnounceChat(Boolean value) {
        this.announceChat = value;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String value) {
        this.background = value;
    }

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
        this.frame = value;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean value) {
        this.hidden = value;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String value) {
        this.icon = value;
    }

    public Boolean getShowToast() {
        return showToast;
    }

    public void setShowToast(Boolean value) {
        this.showToast = value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        this.title = value;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double value) {
        this.x = value;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double value) {
        this.y = value;
    }
}