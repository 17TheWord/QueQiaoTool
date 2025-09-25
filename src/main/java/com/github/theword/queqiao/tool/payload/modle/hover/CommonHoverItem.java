package com.github.theword.queqiao.tool.payload.modle.hover;

public class CommonHoverItem {
    /**
     * Spigot, Forge, Fabric
     *
     * <p>使用int类型时，转一次类型
     */
    String id;

    /** Spigot, Forge, Fabric */
    Integer count;

    /** Spigot */
    String tag;

    /** Velocity */
    String key;

    public CommonHoverItem() {
    }

    public CommonHoverItem(String id, Integer count, String tag, String key) {
        this.id = id;
        this.count = count;
        this.tag = tag;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
