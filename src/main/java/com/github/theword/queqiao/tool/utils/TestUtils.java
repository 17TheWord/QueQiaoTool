package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.handle.ParseJsonToEventService;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.MessagePayload;
import com.github.theword.queqiao.tool.payload.PrivateMessagePayload;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.github.theword.queqiao.tool.response.ResponseEnum;
import com.google.gson.Gson;
import com.google.gson.JsonElement;


public class TestUtils {

    /**
     * Test processing JSON messages
     * <p>The processing service shall be provided by the specific server</p>
     *
     * @param message              json message string
     * @param parseJsonToEventImpl parseJsonToEventImpl
     * @return Response response
     */
    public static Response testParseJsonMessage(String message, ParseJsonToEventService parseJsonToEventImpl) {
        // 组合消息
        Gson gson = GsonUtils.buildGson();
        BasePayload basePayload = gson.fromJson(message, BasePayload.class);
        JsonElement data = basePayload.getData();
        Response response = new Response(200, ResponseEnum.SUCCESS, "success", "No data", basePayload.getEcho());
        try {
            switch (basePayload.getApi()) {
                case "broadcast":
                case "send_msg":
                case "send_actionbar":
                    MessagePayload messageList = gson.fromJson(data, MessagePayload.class);

                    parseJsonToEventImpl.parseMessageListToComponent(messageList.getMessage());

                    break;
                case "send_title":
                    TitlePayload titlePayload = gson.fromJson(data, TitlePayload.class);

                    parseJsonToEventImpl.parseMessageListToComponent(titlePayload.getTitle());

                    if (titlePayload.getSubtitle() != null) {
                        parseJsonToEventImpl.parseMessageListToComponent(titlePayload.getSubtitle());
                    }

                    break;

                case "send_private_msg":
                    PrivateMessagePayload privateMessagePayload = gson.fromJson(data, PrivateMessagePayload.class);

                    parseJsonToEventImpl.parseMessageListToComponent(privateMessagePayload.getMessage());

                    break;
                case "send_command":
                    break;
                default:
                    response.setStatus(ResponseEnum.FAILED);
                    response.setMessage("未知的API：" + basePayload.getApi());
                    break;
            }
        } catch (Exception e) {
            response.setStatus(ResponseEnum.FAILED);
            response.setMessage("在处理消息时出现异常");
            response.setData(e.getMessage());
        }
        return response;
    }

}
