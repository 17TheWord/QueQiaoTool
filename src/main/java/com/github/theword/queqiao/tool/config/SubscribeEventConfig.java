package com.github.theword.queqiao.tool.config;


/**
 * 订阅事件配置
 * <p>默认全部开启</p>
 */
public class SubscribeEventConfig {

    /**
     * 玩家聊天事件
     */
    private boolean playerChat = true;

    /**
     * 玩家死亡事件
     */
    private boolean playerDeath = true;

    /**
     * 玩家加入事件
     */
    private boolean playerJoin = true;

    /**
     * 玩家退出事件
     */
    private boolean playerQuit = true;

    /**
     * 玩家命令事件
     */
    private boolean playerCommand = true;

    /**
     * 玩家进度事件
     */
    private boolean playerAdvancement = true;

    public boolean isPlayerChat() {
        return playerChat;
    }

    public void setPlayerChat(boolean playerChat) {
        this.playerChat = playerChat;
    }

    public boolean isPlayerDeath() {
        return playerDeath;
    }

    public void setPlayerDeath(boolean playerDeath) {
        this.playerDeath = playerDeath;
    }

    public boolean isPlayerJoin() {
        return playerJoin;
    }

    public void setPlayerJoin(boolean playerJoin) {
        this.playerJoin = playerJoin;
    }

    public boolean isPlayerQuit() {
        return playerQuit;
    }

    public void setPlayerQuit(boolean playerQuit) {
        this.playerQuit = playerQuit;
    }

    public boolean isPlayerCommand() {
        return playerCommand;
    }

    public void setPlayerCommand(boolean playerCommand) {
        this.playerCommand = playerCommand;
    }

    public boolean isPlayerAdvancement() {
        return playerAdvancement;
    }

    public void setPlayerAdvancement(boolean playerAdvancement) {
        this.playerAdvancement = playerAdvancement;
    }

    public SubscribeEventConfig() {
    }

    public SubscribeEventConfig(boolean playerChat, boolean playerDeath, boolean playerJoin, boolean playerQuit, boolean playerCommand, boolean playerAdvancement) {
        this.playerChat = playerChat;
        this.playerDeath = playerDeath;
        this.playerJoin = playerJoin;
        this.playerQuit = playerQuit;
        this.playerCommand = playerCommand;
        this.playerAdvancement = playerAdvancement;
    }
}
