package com.github.theword.queqiao.tool.event.model.death;

import com.google.gson.annotations.SerializedName;

/**
 * 死亡信息模型
 */
public class DeathModel {
    private String key;
    private Object[] args;
    @SerializedName("death_message")
    private String deathMessage;

    public DeathModel() {
    }

    public DeathModel(String key, Object[] args, String deathMessage) {
        this.key = key;
        this.args = args;
        this.deathMessage = deathMessage;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public void setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
    }
}
