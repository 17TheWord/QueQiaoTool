package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.payload.*;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.response.Response;
import com.github.theword.queqiao.tool.response.ResponseEnum;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.java_websocket.WebSocket;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

/**
 * 处理协议消息
 */
public class HandleProtocolMessage {

    /**
     * 处理websocket消息
     *
     * @param webSocket WebSocket
     * @param message   websocket消息
     * @return Response {@link Response}
     */
    public Response handleWebSocketJson(WebSocket webSocket, String message) {
        // 组合消息
        debugLog("收到来自 {} 的 WebSocket 消息：{}", webSocket.getRemoteSocketAddress(), message);
        Gson gson = GsonUtils.buildGson();
        BasePayload basePayload = gson.fromJson(message, BasePayload.class);
        JsonElement data = basePayload.getData();
        Response response = new Response(200, ResponseEnum.SUCCESS, "success", "No data", basePayload.getEcho());
        try {
            switch (basePayload.getApi()) {
                case "broadcast":
                case "send_msg":
                    MessagePayload messageList = gson.fromJson(data, MessagePayload.class);
                    GlobalContext.getHandleApiService().handleBroadcastMessage(messageList.getMessage());
                    break;
                case "send_title":
                    TitlePayload titlePayload = gson.fromJson(data, TitlePayload.class);
                    GlobalContext.getHandleApiService().handleSendTitleMessage(titlePayload);
                    break;
                case "send_actionbar":
                    MessagePayload actionMessagePayload = gson.fromJson(data, MessagePayload.class);
                    GlobalContext.getHandleApiService().handleSendActionBarMessage(actionMessagePayload.getMessage());
                    break;
                case "send_private_msg":
                    PrivateMessagePayload privateMessagePayload = gson.fromJson(data, PrivateMessagePayload.class);
                    if ((privateMessagePayload.getNickname() == null || privateMessagePayload.getNickname().isEmpty()) && privateMessagePayload.getUuid() == null) {
                        response.setStatus(ResponseEnum.FAILED);
                        response.setData(PrivateMessageResponse.playerIsNull());
                        response.setMessage(PrivateMessageResponse.playerIsNull().getMessage());
                        return response;
                    }
                    PrivateMessageResponse privateMessageResponse = GlobalContext.getHandleApiService().handleSendPrivateMessage(
                            privateMessagePayload.getNickname(),
                            privateMessagePayload.getUuid(),
                            privateMessagePayload.getMessage()
                    );
                    response.setData(privateMessageResponse);
                    break;
                case "send_command":
                    // TODO Support command
                    response.setCode(500);
                    response.setMessage(basePayload.getApi() + "is not supported now");
                    break;
                default:
                    GlobalContext.getLogger().warn(BaseConstant.UNKNOWN_API + "{}", basePayload.getApi());
                    response.setCode(404);
                    response.setMessage(BaseConstant.UNKNOWN_API + basePayload.getApi());
                    break;
            }
        } catch (Exception e) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.PARSE_MESSAGE_ERROR_ON_MESSAGE, webSocket.getRemoteSocketAddress());
            if (GlobalContext.getConfig().isDebug()) {
                GlobalContext.getLogger().error("Debug模式异常详情", e);
            } else {
                GlobalContext.getLogger().warn(String.valueOf(e.getMessage()));
                if (e.getCause() != null) {
                    GlobalContext.getLogger().warn(String.valueOf(e.getCause().getMessage()));
                }
            }
        }
        return response;
    }
}
