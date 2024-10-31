package com.github.theword.queqiao.tool.handle

import com.github.theword.queqiao.tool.payload.MessageSegment
import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent

/**
 * 该接口只统一方法名
 *
 * 具体返回值类型由服务端自行提供
 */
interface ParseJsonToEventService {
    /**
     * 解析消息列表
     *
     * @param messageList MessageSegment 消息列表
     * @return 服务端具体文本组件
     */
    fun parseMessageListToComponent(messageList: List<MessageSegment>): Any

    /**
     * 解析消息
     *
     * @param message 统一自定义文本组件
     * @return 服务端具体文本组件
     */
    fun parsePerMessageToComponent(message: CommonBaseComponent): Any

    fun parseCommonBaseComponentListToComponent(message: List<CommonBaseComponent>): Any
}
