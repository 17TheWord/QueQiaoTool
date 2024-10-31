package com.github.theword.queqiao.tool.payload.modle.hover

import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent

data class CommonHoverEvent(
    val action: String? = null,
    val text: List<CommonBaseComponent>? = null,
    val item: CommonHoverItem? = null,
    val entity: CommonHoverEntity? = null
) 
