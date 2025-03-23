package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent;

import java.util.List;

/**
 * 公共解析消息接口
 * <p> 服务端均需实现该接口 </p>
 */
public interface ParseJsonToEventService {

    /**
     * 解析消息段列表
     *
     * @param messageList MessageSegment 消息列表 {@link MessageSegment}
     * @return Object 服务端具体文本组件
     */
    Object parseMessageListToComponent(List<MessageSegment> messageList);

    /**
     * 解析消息
     *
     * @param message 统一自定义文本组件 {@link CommonBaseComponent}
     * @return 服务端具体文本组件
     */
    Object parsePerMessageToComponent(CommonBaseComponent message);

    /**
     * 解析基础聊天组件列表
     *
     * @param message 统一自定义聊天组件列表 {@link CommonBaseComponent}
     * @return 服务端具体文本组件
     */
    Object parseCommonBaseComponentListToComponent(List<CommonBaseComponent> message);

}
