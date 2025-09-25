package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.payload.MessageSegment;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import java.util.List;
import java.util.UUID;

/**
 * 公共消息处理接口
 *
 * <p>服务端均需实现该接口
 */
public interface HandleApiService {

  /**
   * API: broadcast / send_msg
   *
   * @param messageList 消息列表
   */
  void handleBroadcastMessage(List<MessageSegment> messageList);

  /**
   * API: send_title
   *
   * @param titlePayload Title
   */
  void handleSendTitleMessage(TitlePayload titlePayload);

  /**
   * API: send_actionbar
   *
   * @param messageList Action Bar 消息列表
   */
  void handleSendActionBarMessage(List<MessageSegment> messageList);

  /**
   * API: send_private_msg
   *
   * @param nickname 目标玩家名
   * @param uuid 目标 UUID
   * @param messageList 消息列表
   * @return 私聊消息响应 {@link PrivateMessageResponse}
   */
  PrivateMessageResponse handleSendPrivateMessage(
      String nickname, UUID uuid, List<MessageSegment> messageList);
}
