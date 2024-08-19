package com.github.theword.queqiao.tool.payload.modle;

import lombok.Data;

@Data
public class CommonHoverItem {
    /**
     * Spigot, Forge, Fabric
     * <p>
     * 使用int类型时，转一次类型
     */
    String id;
    /**
     * Spigot, Forge, Fabric
     */
    Integer count;
    /**
     * Spigot
     */
    String tag;
    /**
     * Velocity
     */
    String key;
}
