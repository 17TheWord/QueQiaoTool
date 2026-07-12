package com.github.theword.queqiao.tool.protocol;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.protocol.handler.BroadcastHandler;
import com.github.theword.queqiao.tool.protocol.handler.GetStatusHandler;
import com.github.theword.queqiao.tool.protocol.handler.SendActionBarHandler;
import com.github.theword.queqiao.tool.protocol.handler.SendCommandHandler;
import com.github.theword.queqiao.tool.protocol.handler.SendPrivateMessageHandler;
import com.github.theword.queqiao.tool.protocol.handler.SendRconCommandHandler;
import com.github.theword.queqiao.tool.protocol.handler.SendTitleHandler;
import com.github.theword.queqiao.tool.response.Response;

import java.util.HashMap;
import java.util.Map;

public class ProtocolRouter {
    private final Map<String, AbstractProtocolHandler<?, ?>> handlers = new HashMap<>();

    public ProtocolRouter() {
        BroadcastHandler broadcastHandler = new BroadcastHandler();
        register(ProtocolConstants.Api.BROADCAST, broadcastHandler);
        register(ProtocolConstants.Api.SEND_MSG, broadcastHandler);
        register(ProtocolConstants.Api.SEND_TITLE, new SendTitleHandler());
        register(ProtocolConstants.Api.SEND_ACTIONBAR, new SendActionBarHandler());
        register(ProtocolConstants.Api.SEND_PRIVATE_MSG, new SendPrivateMessageHandler());
        register(ProtocolConstants.Api.SEND_COMMAND, new SendCommandHandler());
        register(ProtocolConstants.Api.SEND_RCON_COMMAND, new SendRconCommandHandler());
        register(ProtocolConstants.Api.GET_STATUS, new GetStatusHandler());
    }

    public Response route(BasePayload payload) {
        AbstractProtocolHandler<?, ?> handler = handlers.get(payload.getApi());
        if (handler == null) {
            GlobalContext.getLogger().warn(BaseConstant.UNKNOWN_API + "{}", payload.getApi());
            return Response.failed(ProtocolConstants.Status.NOT_FOUND, BaseConstant.UNKNOWN_API + payload.getApi());
        }

        try {
            Object data = handler.handle(payload.getData());
            return Response.success(data);
        } catch (ProtocolException e) {
            return Response.failed(e.getCode(), e.getMessage(), e.getData());
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return Response.failed(ProtocolConstants.Status.INTERNAL_ERROR, message);
        }
    }

    private void register(String api, AbstractProtocolHandler<?, ?> handler) {
        handlers.put(api, handler);
    }
}
