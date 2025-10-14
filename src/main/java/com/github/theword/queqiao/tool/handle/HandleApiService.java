package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.google.gson.JsonElement;

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
     * @param jsonData Json消息
     */
    void handleBroadcastMessage(JsonElement jsonData);

    /**
     * API: send_title
     *
     * @param titlePayload    Title
     * @param subTitlePayload Subtitle
     * @param fadeIn          淡入时间(ticks)
     * @param stay            停留时间(ticks)
     * @param fadeOut         淡出时间(ticks)
     */
    void handleSendTitleMessage(JsonElement titlePayload, JsonElement subTitlePayload, int fadeIn, int stay, int fadeOut);

    /**
     * API: send_actionbar
     *
     * @param jsonData Json消息
     */
    void handleSendActionBarMessage(JsonElement jsonData);

    /**
     * API: send_private_msg
     *
     * @param nickname 目标玩家名
     * @param uuid     目标 UUID
     * @param jsonData Json消息
     * @return 私聊消息响应 {@link PrivateMessageResponse}
     */
    PrivateMessageResponse handleSendPrivateMessage(String nickname, UUID uuid, JsonElement jsonData);
}
