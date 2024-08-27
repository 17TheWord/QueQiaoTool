package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.modle.component.CommonTextComponent;

import java.util.List;

public interface ParseJsonToEventService {

    Object parseMessageListToComponent(List<MessageSegment> messageList);

    Object parsePerMessageToComponent(CommonTextComponent message);

}
