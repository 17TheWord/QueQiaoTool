package com.github.theword.queqiao.tool.protocol;

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
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 协议路由器
 *
 * <p>把 {@code api} 字段映射到对应的 {@link AbstractProtocolHandler}。
 *
 * <p><b>线程安全</b>：处理器表只在构造阶段写入（{@code register} 为 private 且仅构造器调用），
 * 之后只读，因此本类构造后即不可变、可被多连接并发使用，且<b>不含任何锁</b>。
 *
 * <p><b>不依赖全局状态</b>：日志实现由构造器注入，不再访问 {@code GlobalContext.getLogger()}，
 * 使路由与处理器层可脱离全局上下文独立测试。
 *
 * @since 0.6.11
 */
public class ProtocolRouter {

    private final Logger logger;

    private final Map<String, AbstractProtocolHandler<?, ?>> handlers = new HashMap<>();

    /**
     * 构造路由器
     *
     * @param logger 日志实现，不得为 null
     */
    public ProtocolRouter(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");

        BroadcastHandler broadcastHandler = new BroadcastHandler(logger);
        register(ProtocolConstants.Api.BROADCAST, broadcastHandler);
        register(ProtocolConstants.Api.SEND_MSG, broadcastHandler);
        register(ProtocolConstants.Api.SEND_TITLE, new SendTitleHandler(logger));
        register(ProtocolConstants.Api.SEND_ACTIONBAR, new SendActionBarHandler(logger));
        register(ProtocolConstants.Api.SEND_PRIVATE_MSG, new SendPrivateMessageHandler(logger));
        register(ProtocolConstants.Api.SEND_COMMAND, new SendCommandHandler(logger));
        register(ProtocolConstants.Api.SEND_RCON_COMMAND, new SendRconCommandHandler(logger));
        register(ProtocolConstants.Api.GET_STATUS, new GetStatusHandler(logger));
    }

    /**
     * 路由并处理一个请求
     *
     * <p>状态码语义：
     * <ul>
     *     <li>{@code 400} —— 请求本身不合法（payload 为 null、缺少 api 字段、负载解析失败）</li>
     *     <li>{@code 404} —— api 未注册</li>
     *     <li>处理器抛出的 {@link ProtocolException} —— 使用其自带状态码（如 400 / 500）</li>
     *     <li>{@code 500} —— 真正的服务端未预期异常</li>
     * </ul>
     *
     * @param payload 请求负载，允许为 null
     * @return 响应，永不为 null
     */
    public Response route(BasePayload payload) {
        if (payload == null) {
            this.logger.warn("请求负载为空，已拒绝");
            return Response.failed(ProtocolConstants.Status.BAD_REQUEST, ProtocolConstants.Message.PARSE_MESSAGE_FAILED);
        }

        String api = payload.getApi();
        if (api == null || api.trim().isEmpty()) {
            this.logger.warn("请求缺少 api 字段，已拒绝");
            return Response.failed(ProtocolConstants.Status.BAD_REQUEST, ProtocolConstants.Message.MISSING_API);
        }

        AbstractProtocolHandler<?, ?> handler = handlers.get(api);
        if (handler == null) {
            this.logger.warn(BaseConstant.UNKNOWN_API + "{}", api);
            return Response.failed(ProtocolConstants.Status.NOT_FOUND, BaseConstant.UNKNOWN_API + api);
        }

        try {
            Object data = handler.handle(payload.getData());
            return Response.success(data);
        } catch (ProtocolException e) {
            return Response.failed(e.getCode(), e.getMessage(), e.getData());
        } catch (Exception e) {
            // 只有真正的内部异常才归 500；此处记录堆栈但不回传任何请求内容
            this.logger.error("处理 api={} 的请求时发生未预期异常", api, e);
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return Response.failed(ProtocolConstants.Status.INTERNAL_ERROR, message);
        }
    }

    private void register(String api, AbstractProtocolHandler<?, ?> handler) {
        handlers.put(api, handler);
    }
}
