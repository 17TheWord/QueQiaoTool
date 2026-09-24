package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

/**
 * WebSocket 服务端
 *
 * <p><b>握手校验</b>：{@link #onOpen(WebSocket, ClientHandshake)} 依次校验
 * {@code x-self-name} → {@code x-client-origin} → {@code Authorization}，
 * 任一项失败即关闭连接。
 *
 * <p><b>为什么 onOpen 必须自己兜住异常</b>：Java-WebSocket 的 {@code WebSocketImpl.open()} 实现为
 * <pre>
 * readyState = OPEN;
 * try { wsl.onWebsocketOpen(this, d); }
 * catch (RuntimeException e) { wsl.onWebsocketError(this, e); }   // 只上报，不关闭、不重抛
 * </pre>
 * 也就是说 {@code onOpen} 抛出的异常会被库吞掉、连接保持 {@code OPEN}，
 * 于是"未通过鉴权"的连接仍会被 {@link #onMessage(WebSocket, String)} 正常处理，
 * 构成<b>鉴权旁路</b>。因此 {@code onOpen} 内部任何异常都必须主动关闭连接，
 * 绝不允许异常逃出去。（该行为已用一次性实验实证：客户端未被关闭、服务端仍持有该连接。）
 *
 * <p><b>关于连接数与帧大小上限（已评估，决定不设置）</b>
 * <ul>
 *     <li><b>总连接数上限</b>：{@code WebSocketServer.setMaxPendingConnections(int)} 限制的是
 *         "尚未完成握手的挂起连接"而非总连接数，语义容易被误解；本项目默认绑定回环地址
 *         且强制 token 鉴权，暴露面很小，故不新增该配置。</li>
 *     <li><b>帧大小上限</b>：Java-WebSocket 的 {@code Draft_6455} 默认 {@code maxFrameSize}
 *         为 {@code Integer.MAX_VALUE}（即实际无上限）。评估后仍不设置，理由：
 *         需要有效 token 才能连上；默认仅监听回环地址；
 *         且帧大小上限<b>无法</b>阻止已鉴权客户端改用大量小消息实施 DoS。
 *         代价则是新增一条拒绝路径与一个配置项，收益无法证明。</li>
 * </ul>
 * 若将来需要对外网暴露，应重新评估这两项。
 *
 * @since 0.6.11
 */
public class WsServer extends WebSocketServer {

    /**
     * 连接丢失检测周期（秒）
     *
     * <p>显式设置，避免依赖 Java-WebSocket 的默认值。该检测会向客户端发送 ping，
     * 超时未收到 pong 时以 {@code remote == false} 关闭连接，属正常的失联回收。
     */
    private static final int CONNECTION_LOST_TIMEOUT_SECONDS = 60;

    /**
     * 握手校验失败使用的关闭码（策略违规）
     */
    private static final int CLOSE_CODE_POLICY_VIOLATION = 1008;

    /**
     * 服务端内部错误使用的关闭码
     */
    private static final int CLOSE_CODE_INTERNAL_ERROR = 1011;

    /**
     * 服务端级错误（无具体客户端）时用于日志的地址占位符
     */
    private static final String SERVER_SIDE_ADDRESS = "<server>";

    /**
     * 无法获取远端地址时用于日志的占位符
     */
    private static final String UNKNOWN_ADDRESS = "<unknown>";

    private final String hostName;
    private final int port;
    private final Logger logger;
    private final String serverName;
    private final String accessToken;
    private final boolean enabled;
    private final HandleProtocolMessage handleProtocolMessage;

    /**
     * 构造函数
     *
     * @param address              地址
     * @param logger               日志实现
     * @param handleProtocolMessage 协议分发入口（由 QueQiaoRuntime 创建，与 Client 共享同一实例）
     * @param serverName           服务器名称，为 null 时按空串处理
     * @param accessToken          访问令牌（可选）；为 null 或空串表示不鉴权
     * @param enabled              是否启用协议消息处理
     */
    public WsServer(
                    InetSocketAddress address, Logger logger, HandleProtocolMessage handleProtocolMessage, String serverName, String accessToken, boolean enabled) {
        super(address);
        super.setReuseAddr(true);
        this.logger = logger;
        this.hostName = address.getHostName();
        this.port = address.getPort();
        // 归一化 null：onOpen 会直接对这两个字段调用 isEmpty()，
        // 若平台实现传入 null（而非空串）会在握手阶段抛 NPE，
        // 而该异常会被库吞掉并保留连接 —— 即上面类注释所说的鉴权旁路。
        this.serverName = serverName == null ? "" : serverName;
        this.accessToken = accessToken == null ? "" : accessToken;
        this.enabled = enabled;
        this.handleProtocolMessage = handleProtocolMessage;
        this.setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SECONDS);
    }

    /**
     * 获取客户端地址
     *
     * <p>用 {@link InetSocketAddress#getHostString()} 与 {@link InetSocketAddress#getPort()}
     * 直接取地址，不再依赖 {@code InetSocketAddress.toString()} 的具体格式做字符串清理。
     *
     * <p>两级空值防护：
     * <ul>
     *     <li>{@code webSocket} 为 null —— 服务端级致命错误（如 selector 异常）时，
     *         库会以 {@code onError(null, e)} 回调，此时没有具体客户端</li>
     *     <li>远端地址为 null —— {@code WebSocketImpl} 在 {@code channel} 为空时返回 null</li>
     * </ul>
     *
     * @param webSocket 客户端，允许为 null
     * @return 形如 {@code 127.0.0.1:49271} 的地址；无法获取时返回占位符
     */
    private static String getClientAddress(WebSocket webSocket) {
        if (webSocket == null) {
            return SERVER_SIDE_ADDRESS;
        }
        InetSocketAddress remoteAddress = webSocket.getRemoteSocketAddress();
        if (remoteAddress == null) {
            return UNKNOWN_ADDRESS;
        }
        return remoteAddress.getHostString() + ":" + remoteAddress.getPort();
    }

    private String getHeaderOrQueryParam(ClientHandshake clientHandshake, String name) {
        String headerValue = clientHandshake.getFieldValue(name);
        if (!headerValue.isEmpty()) {
            return headerValue;
        }

        String resource = clientHandshake.getResourceDescriptor();
        int queryStart = resource == null ? -1 : resource.indexOf('?');
        if (queryStart < 0 || queryStart + 1 >= resource.length()) {
            return "";
        }

        String query = resource.substring(queryStart + 1);
        for (String pair : query.split("&")) {
            int splitIndex = pair.indexOf('=');
            String key = splitIndex >= 0 ? pair.substring(0, splitIndex) : pair;
            String value = splitIndex >= 0 ? pair.substring(splitIndex + 1) : "";
            try {
                if (URLDecoder.decode(key, StandardCharsets.UTF_8.name()).equals(name)) {
                    return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                }
            } catch (IllegalArgumentException | UnsupportedEncodingException ignored) {
            }
        }
        return "";
    }

    /**
     * 当客户端连接时执行 连接将依次检验 x-self-name；x-client-origin；Authorization字段
     *
     * <p>本方法<b>不允许任何异常逃出</b>，原因见类注释（库会吞掉异常并保留连接）。
     *
     * @param webSocket       客户端
     * @param clientHandshake 客户端握手信息
     */
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        try {
            if (!validateHandshake(webSocket, clientHandshake)) {
                return;
            }
            this.logger.info(WebsocketConstantMessage.Server.CLIENT_CONNECTED, getClientAddress(webSocket));
        } catch (RuntimeException e) {
            this.logger.error("处理连接握手时发生未预期异常，已主动关闭该连接。来自：{}", getClientAddress(webSocket), e);
            closeQuietly(webSocket, CLOSE_CODE_INTERNAL_ERROR, "Internal server error during handshake");
        }
    }

    /**
     * 校验握手字段
     *
     * @param webSocket       客户端
     * @param clientHandshake 客户端握手信息
     * @return true 表示校验通过；false 表示校验失败且已关闭连接
     */
    private boolean validateHandshake(WebSocket webSocket, ClientHandshake clientHandshake) {
        String originServerName = getHeaderOrQueryParam(clientHandshake, "x-self-name");
        if (originServerName.isEmpty()) {
            this.logger.warn(
                    WebsocketConstantMessage.Server.MISSING_SERVER_NAME_HEADER, getClientAddress(webSocket));
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "Missing X-Self-name Header");
            return false;
        }

        String clientOrigin = getHeaderOrQueryParam(clientHandshake, "x-client-origin");
        if (clientOrigin.equalsIgnoreCase("minecraft")) {
            this.logger.warn(
                    WebsocketConstantMessage.Server.INVALID_CLIENT_ORIGIN_HEADER, getClientAddress(webSocket));
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "X-Client-Origin Header cannot be minecraft");
            return false;
        }

        String decodedServerName;
        try {
            decodedServerName = URLDecoder.decode(originServerName, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            this.logger.error(
                    WebsocketConstantMessage.Server.SERVER_NAME_DECODE_FAILED_HEADER, getClientAddress(webSocket), originServerName, e.getMessage());
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "X-Self-name Header decode failed");
            return false;
        }

        if (decodedServerName.isEmpty()) {
            this.logger.warn(
                    WebsocketConstantMessage.Server.SERVER_NAME_PARSE_FAILED_HEADER, getClientAddress(webSocket));
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "X-Self-name Header cannot be empty");
            return false;
        }

        if (!decodedServerName.equals(this.serverName)) {
            this.logger.warn(
                    WebsocketConstantMessage.Server.INVALID_SERVER_NAME_HEADER, getClientAddress(webSocket), decodedServerName);
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "X-Self-name Header is wrong");
            return false;
        }

        String accessToken = getHeaderOrQueryParam(clientHandshake, "Authorization");
        if (!this.accessToken.isEmpty() && !accessToken.equals("Bearer " + this.accessToken)) {
            // 安全：绝不记录客户端提交的 Authorization / accessToken / Bearer token 内容，仅记录来源地址。
            this.logger.warn(
                    WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER, getClientAddress(webSocket));
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("认证失败详情：客户端是否携带 Authorization 头部 = {}", !accessToken.isEmpty());
            }
            closeQuietly(webSocket, CLOSE_CODE_POLICY_VIOLATION, "Authorization Header is wrong");
            return false;
        }

        return true;
    }

    /**
     * 关闭连接且不抛出异常
     *
     * <p>用于异常兜底路径：此时再抛异常会掩盖原始问题。
     *
     * @param webSocket 客户端，允许为 null
     * @param code      关闭码
     * @param reason    关闭原因
     */
    private static void closeQuietly(WebSocket webSocket, int code, String reason) {
        if (webSocket == null) {
            return;
        }
        try {
            webSocket.close(code, reason);
        } catch (RuntimeException ignored) {
            // 关闭失败不再上抛：调用方已在异常兜底路径上
        }
    }

    /**
     * 当客户端断开连接时执行
     *
     * @param webSocket 客户端
     * @param code      关闭码
     * @param reason    关闭原因
     * @param remote    是否是远程关闭
     */
    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        String closeReason = remote ? WebsocketConstantMessage.Server.CLIENT_DISCONNECTED : WebsocketConstantMessage.Server.CLIENT_HAD_BEEN_DISCONNECTED;
        this.logger.info(closeReason, getClientAddress(webSocket));
    }

    /**
     * 当接收到客户端的消息时执行
     *
     * <p><b>协议约定：只支持文本帧。</b>二进制帧由 Java-WebSocket 的
     * {@code onMessage(WebSocket, ByteBuffer)} 空实现静默忽略——本项目<b>有意不覆写</b>它：
     * 覆写只能额外打一条日志、不改变任何行为，反而会在客户端持续发送二进制帧时刷屏。
     * 若排查"客户端称已发送但服务端无响应"，这是一个需要确认的方向。
     *
     * @param webSocket 客户端
     * @param message   消息
     */
    @Override
    public void onMessage(WebSocket webSocket, String message) {
        if (this.enabled) {
            String response = this.handleProtocolMessage.handleWebsocketJson(webSocket, message);
            if (response != null && !response.isEmpty()) {
                webSocket.send(response);
            }
        }
    }

    /**
     * 当连接出现异常时执行
     *
     * @param webSocket 客户端，服务端级致命错误时为 null
     * @param exception 异常
     */
    @Override
    public void onError(WebSocket webSocket, Exception exception) {
        this.logger.warn(
                WebsocketConstantMessage.Server.CONNECTION_ERROR,
                getClientAddress(webSocket),
                resolveErrorMessage(exception));
    }

    /**
     * 解析异常消息
     *
     * <p>{@code getMessage()} 可能为 null，此时回退到类名，避免日志出现无意义的 {@code null}。
     *
     * @param exception 异常，允许为 null
     * @return 可读的错误信息
     */
    private static String resolveErrorMessage(Exception exception) {
        if (exception == null) {
            return "unknown";
        }
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    /** 当服务器启动时执行 */
    @Override
    public void onStart() {
        this.logger.info(WebsocketConstantMessage.Server.SERVER_STARTING, hostName, port);
    }
}
