package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent;

import java.util.List;

/**
 * 该接口只统一方法名
 * <p>具体返回值类型由服务端自行提供</p>
 */
public interface ParseJsonToEventService {

    /**
     * 解析消息列表
     *
     * @param messageList MessageSegment 消息列表
     * @return 服务端具体文本组件
     */
    Object parseMessageListToComponent(List<MessageSegment> messageList);

    /**
     * 解析消息
     *
     * @param message 统一自定义文本组件
     * @return 服务端具体文本组件
     */
    Object parsePerMessageToComponent(CommonBaseComponent message);

    Object parseCommonBaseComponentListToComponent(List<CommonBaseComponent> message);

}
