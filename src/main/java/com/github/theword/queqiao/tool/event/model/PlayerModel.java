package com.github.theword.queqiao.tool.event.model;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

/**
 * 玩家模型
 */
public class PlayerModel {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * UUID
     */
    private UUID uuid;

    /**
     * 网络地址
     */
    private String address;

    /**
     * 血量
     */
    private Double health;

    /**
     * 最大血量
     *
     * <p>不支持的服务端：Folia、Spigot、Paper
     */
    @SerializedName("max_health")
    private Double maxHealth;

    /**
     * 经验值
     */
    @SerializedName("experience_level")
    private Integer experienceLevel;

    /**
     * 经验进度
     */
    @SerializedName("experience_progress")
    private Double experienceProgress;

    /**
     * 经验总值
     */
    @SerializedName("total_experience")
    private Integer totalExperience;


    /**
     * 是否OP
     */
    @SerializedName("is_op")
    private Boolean isOp;

    /**
     * 行走速度
     */
    @SerializedName("walk_speed")
    private Double walkSpeed;

    /**
     * X轴
     */
    private Double x;

    /**
     * Y轴
     */
    private Double y;

    /**
     * Z轴
     */
    private Double z;

    /**
     * 判断两个玩家是否为同一个玩家
     *
     * <p>玩家对象中，nickname 和 uuid 至少有一个不为空
     *
     * <p>首先 判断 uuid 是否相等，若相等则返回 true
     *
     * <p>若不相等，则判断 nickname 是否相等，若相等则返回 true
     *
     * @param o 对比对象
     * @return 是否为同一个玩家
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlayerModel)) return false;
        if (this == o) return true;
        if (uuid != null && uuid.equals(((PlayerModel) o).uuid)) return true;
        return nickname != null && nickname.equals(((PlayerModel) o).nickname);
    }

    @Override
    public int hashCode() {
        return nickname.hashCode();
    }

    public PlayerModel() {
    }

    public PlayerModel(String nickname) {
        this.nickname = nickname;
    }

    public PlayerModel(String nickname, UUID uuid) {
        this.nickname = nickname;
        this.uuid = uuid;
    }

    public PlayerModel(String nickname, UUID uuid, String address, Double health, Double maxHealth, Integer experienceLevel, Double experienceProgress, Integer totalExperience, Boolean isOp, Double walkSpeed, Double x, Double y, Double z) {
        this.nickname = nickname;
        this.uuid = uuid;
        this.address = address;
        this.health = health;
        this.maxHealth = maxHealth;
        this.experienceLevel = experienceLevel;
        this.experienceProgress = experienceProgress;
        this.totalExperience = totalExperience;
        this.isOp = isOp;
        this.walkSpeed = walkSpeed;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getHealth() {
        return health;
    }

    public void setHealth(Double health) {
        this.health = health;
    }

    public Double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(Double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public Integer getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(Integer experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public Double getExperienceProgress() {
        return experienceProgress;
    }

    public void setExperienceProgress(Double experienceProgress) {
        this.experienceProgress = experienceProgress;
    }

    public Integer getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(Integer totalExperience) {
        this.totalExperience = totalExperience;
    }

    public Boolean getOp() {
        return isOp;
    }

    public void setOp(Boolean op) {
        isOp = op;
    }

    public Double getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(Double walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }
}
