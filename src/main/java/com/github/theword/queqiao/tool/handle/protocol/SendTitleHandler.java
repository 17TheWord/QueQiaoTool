package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.constant.ApiConstants;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.payload.TitlePayload;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;

/**
 * 处理标题消息 API。
 */
public class SendTitleHandler implements ProtocolHandler {
    private final Gson gson;
    private final HandleApiService handleApiService;

    public SendTitleHandler(Gson gson, HandleApiService handleApiService) {
        this.gson = gson;
        this.handleApiService = handleApiService;
    }

    @Override
    public Response handle(BasePayload payload) {
        TitlePayload titlePayload = gson.fromJson(payload.getData(), TitlePayload.class);
        if ((titlePayload.getTitle() == null || titlePayload.getTitle().isJsonNull()) && (titlePayload.getSubtitle() == null || titlePayload.getSubtitle().isJsonNull())) {
            return Response.failed(ApiConstants.Code.BAD_REQUEST, ApiConstants.Message.TITLE_AND_SUBTITLE_EMPTY);
        }
        handleApiService.handleSendTitleMessage(titlePayload.getTitle(), titlePayload.getSubtitle(), titlePayload.getFadeIn(), titlePayload.getStay(), titlePayload.getFadeOut());
        return Response.success();
    }
}
