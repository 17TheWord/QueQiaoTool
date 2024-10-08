package com.github.theword.queqiao.tool.payload.modle.hover;

import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent;
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
    List<CommonBaseComponent> name;
    /**
     * Velocity
     */
    UUID uuid;
    /**
     * Velocity
     */
    String key;
}
