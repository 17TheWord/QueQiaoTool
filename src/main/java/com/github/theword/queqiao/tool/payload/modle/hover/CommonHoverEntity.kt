package com.github.theword.queqiao.tool.payload.modle.hover

import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent
import java.util.UUID

data class CommonHoverEntity(
    /**
     * Spigot, Forge, Fabric
     */
    var type: String? = null,

    /**
     * Spigot
     */
    var id: String? = null,

    /**
     * Spigot, Forge, Fabric
     */
    var name: List<CommonBaseComponent>? = null,

    /**
     * Velocity
     */
    var uuid: UUID? = null,

    /**
     * Velocity
     */
    var key: String? = null,
) {
    override fun toString(): String {
        return "CommonHoverEntity(type=$type, id=$id, name=$name, uuid=$uuid, key=$key)"
    }
}
