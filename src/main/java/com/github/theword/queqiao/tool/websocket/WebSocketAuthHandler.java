package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手前鉴权处理器。
 */
final class WebSocketAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    static final AttributeKey<Boolean> AUTH_PASSED = AttributeKey.valueOf("ws-auth-passed");
    private static final String HEADER_SELF_NAME = "x-self-name";
    private static final String HEADER_CLIENT_ORIGIN = "x-client-origin";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String CLIENT_ORIGIN_MINECRAFT = "minecraft";
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final WsServer wsServer;

    WebSocketAuthHandler(WsServer wsServer) {
        this.wsServer = wsServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String upgrade = request.headers().get(HttpHeaderNames.UPGRADE);
        if (upgrade == null || !HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade)) {
            reject(ctx, HttpResponseStatus.BAD_REQUEST, "Not websocket request");
            return;
        }

        QueryStringDecoder decoder;
        try {
            decoder = new QueryStringDecoder(request.uri());
        } catch (RuntimeException e) {
            reject(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid websocket uri");
            return;
        }

        if (!normalizeWebSocketPath(request, decoder)) {
            reject(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid websocket uri");
            return;
        }

        String originServerName = getHeaderOrQueryParam(request, decoder, HEADER_SELF_NAME);
        if (originServerName.isEmpty()) {
            this.wsServer.getInternalLogger().warn(
                    WebsocketConstantMessage.Server.MISSING_SERVER_NAME_HEADER, this.wsServer.getClientAddress(ctx.channel())
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "Missing X-Self-name Header");
            return;
        }

        String clientOrigin = getHeaderOrQueryParam(request, decoder, HEADER_CLIENT_ORIGIN);
        if (CLIENT_ORIGIN_MINECRAFT.equalsIgnoreCase(clientOrigin)) {
            this.wsServer.getInternalLogger().warn(
                    WebsocketConstantMessage.Server.INVALID_CLIENT_ORIGIN_HEADER, this.wsServer.getClientAddress(ctx.channel())
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Client-Origin Header cannot be minecraft");
            return;
        }

        String decodedServerName;
        try {
            decodedServerName = URLDecoder.decode(originServerName, StandardCharsets.UTF_8.name());
        } catch (IllegalArgumentException | UnsupportedEncodingException e) {
            this.wsServer.getInternalLogger().error(
                    WebsocketConstantMessage.Server.SERVER_NAME_DECODE_FAILED_HEADER, this.wsServer.getClientAddress(ctx.channel()), originServerName, e.getMessage()
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header decode failed");
            return;
        }

        if (decodedServerName.isEmpty()) {
            this.wsServer.getInternalLogger().warn(
                    WebsocketConstantMessage.Server.SERVER_NAME_PARSE_FAILED_HEADER, this.wsServer.getClientAddress(ctx.channel())
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header cannot be empty");
            return;
        }

        if (!decodedServerName.equals(this.wsServer.getServerNameValue())) {
            this.wsServer.getInternalLogger().warn(
                    WebsocketConstantMessage.Server.INVALID_SERVER_NAME_HEADER, this.wsServer.getClientAddress(ctx.channel()), decodedServerName
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header is wrong");
            return;
        }

        String authorization = getHeaderOrQueryParam(request, decoder, HEADER_AUTHORIZATION);
        if (!this.wsServer.getAccessTokenValue().isEmpty() && !(AUTHORIZATION_PREFIX + this.wsServer.getAccessTokenValue()).equals(authorization)) {
            this.wsServer.getInternalLogger().warn(
                    WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER, this.wsServer.getClientAddress(ctx.channel()), authorization
            );
            reject(ctx, HttpResponseStatus.FORBIDDEN, "Authorization Header is wrong");
            return;
        }

        ctx.channel().attr(AUTH_PASSED).set(Boolean.TRUE);
        ctx.fireChannelRead(request.retain());
    }

    /**
     * 标准化 WebSocket 请求路径，仅允许根路径和配置路径。
     */
    private boolean normalizeWebSocketPath(FullHttpRequest request, QueryStringDecoder decoder) {
        try {
            String path = decoder.path();
            String websocketPath = this.wsServer.getWebsocketPath();

            if (!websocketPath.equals(path) && !"/".equals(path)) {
                return false;
            }

            String rawQuery = decoder.rawQuery();
            String normalizedUri = rawQuery == null || rawQuery.isEmpty() ? websocketPath : websocketPath + "?" + rawQuery;
            request.setUri(normalizedUri);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 优先读取 Header，缺失时回退到查询参数。
     */
    private String getHeaderOrQueryParam(FullHttpRequest request, QueryStringDecoder decoder, String name) {
        String headerValue = request.headers().get(name);
        if (headerValue != null && !headerValue.isEmpty()) {
            return headerValue;
        }

        Map<String, List<String>> parameters = decoder.parameters();
        List<String> values = parameters.get(name);
        if (values == null || values.isEmpty()) {
            return "";
        }
        String value = values.get(0);
        return value == null ? "" : value;
    }

    private void reject(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        ByteBuf content = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
