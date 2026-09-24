package com.github.theword.queqiao.tool.handle;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;

import com.github.theword.queqiao.tool.constant.CommonConstants;
import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.payload.BasePayload;
import com.github.theword.queqiao.tool.protocol.ProtocolRouter;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import java.util.HashMap;

/**
 * 处理协议消息
 *
 * <p>这是<b>传输无关</b>的协议分发入口：WebSocket（以及未来的 HTTP）都复用同一套解析与路由。
 *
 * <p><b>生命周期与线程安全</b>：本类由 {@code QueQiaoRuntime} 创建唯一实例并注入给各传输层
 * （{@code WebsocketManager} → 各 {@code WsClient} / {@code WsServer}），
 * 因此会被多个连接、多个线程并发调用。
 *
 * <p>本类构造后即不可变：字段全部 {@code final}，
 * {@code ProtocolRouter} 的处理器表也只在构造阶段写入、之后只读；
 * 各处理器实现必须无状态（见 {@code AbstractProtocolHandler}）。
 * 因此并发调用是安全的，且<b>不存在锁</b>——不同连接之间不会相互串行化。
 *
 * <p>顺序说明：单个连接内的请求处理顺序由该连接的读线程串行保证，
 * 与本类实例数量无关。
 *
 * @since 0.6.11
 */
public class HandleProtocolMessage {

    private final Gson gson;
    private final Logger logger;
    private final ProtocolRouter protocolRouter;

    public HandleProtocolMessage(Logger logger, Gson gson) {
        this.logger = logger;
        this.gson = gson;
        this.protocolRouter = new ProtocolRouter();
    }

    /**
     * 消息来源枚举
     */
    private enum MessageSource {
        HTTP, WEBSOCKET;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    /**
     * WebSocket入口，处理JSON字符串
     *
     * @param webSocket      WebSocket连接
     * @param rawJsonMessage 收到的JSON字符串
     * @return 响应的JSON字符串
     */
    public String handleWebsocketJson(WebSocket webSocket, String rawJsonMessage) {
        Response response = this.handle(rawJsonMessage, webSocket.getRemoteSocketAddress().toString(), MessageSource.WEBSOCKET);
        return gson.toJson(response);
    }

    /**
     * Http，处理JSON字符串
     *
     * <p><b>保留入口</b>：当前版本尚未接入 HTTP 传输层，本方法暂无调用方。
     * 它是有意保留的扩展点——协议分发与传输方式无关，
     * 后续接入 HTTP 时应直接复用本入口，而不是另起一套解析与路由。
     * 因此请勿将其当作"死代码"清理。
     *
     * @param rawJsonMessage 收到的JSON字符串
     * @return 响应的JSON字符串
     */
    public String handleHttpJson(String rawJsonMessage) {
        Response response = this.handle(rawJsonMessage, CommonConstants.Text.EMPTY, MessageSource.HTTP);
        return gson.toJson(response);
    }

    private Response handle(String rawJsonMessage, String address, MessageSource source) {
        debugLog("收到来自 {} 的 {} 消息：{}", address, source.toString(), rawJsonMessage);
        try {
            BasePayload basePayload = gson.fromJson(rawJsonMessage, BasePayload.class);
            Response response = this.parseAndHandle(basePayload);
            response.setApi(basePayload.getApi());
            response.setEcho(basePayload.getEcho());
            return response;
        } catch (Exception e) {
            this.logger.error("解析来自 {} 的 webSocket 消息时出现问题，消息内容：{}", address, rawJsonMessage);
            this.logger.error("错误信息：", e);
            HashMap<String, String> data = new HashMap<>();
            data.put("rawJsonMessage", rawJsonMessage);
            return Response.failed(ProtocolConstants.Status.INTERNAL_ERROR, ProtocolConstants.Message.PARSE_MESSAGE_FAILED, data, null);
        }
    }

    /**
     * 业务处理核心：接收原始 JSON 字符串并解析与处理
     *
     * @return Response 处理结果
     */
    public Response parseAndHandle(BasePayload basePayload) {
        return protocolRouter.route(basePayload);
    }
}
