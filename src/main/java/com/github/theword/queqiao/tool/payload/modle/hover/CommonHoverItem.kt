package com.github.theword.queqiao.tool.payload.modle.hover


data class CommonHoverItem(
    /**
     * Spigot, Forge, Fabric
     *
     *
     * 使用int类型时，转一次类型
     */
    var id: String? = null,

    /**
     * Spigot, Forge, Fabric
     */
    var count: Int? = null,

    /**
     * Spigot
     */
    var tag: String? = null,

    /**
     * Velocity
     */
    var key: String? = null
)