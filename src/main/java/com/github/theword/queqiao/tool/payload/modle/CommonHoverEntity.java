package com.github.theword.queqiao.tool.payload.modle;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CommonHoverEntity {
    /**
     * Spigot, Forge, Fabric
     */
    String type;
    /**
     * Spigot
     */
    String id;
    /**
     * Spigot, Forge, Fabric
     */
    List<? extends CommonBaseComponent> name;
    /**
     * Velocity
     */
    UUID uuid;
    /**
     * Velocity
     */
    String key;
}
