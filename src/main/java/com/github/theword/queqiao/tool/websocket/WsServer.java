package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.utils.ServerPropertiesTool;
import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Netty WebSocket 服务端。
 *
 * <p>支持两类流量：
 * <p>1. WebSocket：按既有协议鉴权并处理消息
 * <p>2. 非 WebSocket：可按配置转发到本地 MC 主端口
 */
public class WsServer {

    private static final int MAX_DETECT_BYTES = 8192;
    private static final String WEBSOCKET_PATH = "/minecraft/ws";
    private static final String LOCALHOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 25565;
    private static final AttributeKey<Boolean> AUTH_PASSED = AttributeKey.valueOf("ws-auth-passed");

    private final InetSocketAddress address;
    private final String hostName;
    private final int port;
    private final Logger logger;
    private final String serverName;
    private final String accessToken;
    private final boolean enabled;
    private final boolean forward;
    private final HandleProtocolMessage handleProtocolMessage;

    /** 当前已完成握手并通过鉴权的 WebSocket 连接 */
    private final Set<Channel> connections = ConcurrentHashMap.newKeySet();

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;
    private volatile int proxyTargetPort = DEFAULT_SERVER_PORT;

    public WsServer(
                    InetSocketAddress address, Logger logger, Gson gson, String serverName, String accessToken, boolean enabled) {
        this(address, logger, gson, serverName, accessToken, enabled, false);
    }

    public WsServer(
                    InetSocketAddress address, Logger logger, Gson gson, String serverName, String accessToken, boolean enabled, boolean forward) {
        this.address = address;
        this.hostName = address.getHostName();
        this.port = address.getPort();
        this.logger = logger;
        this.serverName = serverName;
        this.accessToken = accessToken == null ? "" : accessToken;
        this.enabled = enabled;
        this.forward = forward;
        this.handleProtocolMessage = new HandleProtocolMessage(logger, gson);
    }

    /** 启动服务端 */
    public synchronized void start() {
        if (this.serverChannel != null && this.serverChannel.isActive()) {
            return;
        }

        this.proxyTargetPort = resolveServerPort();

        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(this.bossGroup, this.workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_REUSEADDR, true).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new ProtocolSwitchHandler(WsServer.this));
            }
        });

        try {
            ChannelFuture bindFuture = bootstrap.bind(this.address).syncUninterruptibly();
            if (!bindFuture.isSuccess()) {
                throw new IllegalStateException("bind failed", bindFuture.cause());
            }
            this.serverChannel = bindFuture.channel();
            this.logger.info(WebsocketConstantMessage.Server.SERVER_STARTING, this.hostName, getPort());
        } catch (RuntimeException e) {
            releaseStartResources();
            throw e;
        }
    }

    /** 停止服务端并释放资源 */
    public synchronized void stop(int timeout, String reason) throws InterruptedException {
        for (Channel channel : new ArrayList<Channel>(this.connections)) {
            closeChannel(channel, reason);
        }
        this.connections.clear();

        if (this.serverChannel != null) {
            this.serverChannel.close().sync();
            this.serverChannel = null;
        }

        long timeoutMillis = Math.max(timeout, 0);

        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully(0, timeoutMillis, TimeUnit.MILLISECONDS).sync();
            this.workerGroup = null;
        }

        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully(0, timeoutMillis, TimeUnit.MILLISECONDS).sync();
            this.bossGroup = null;
        }
    }

    /**
     * 获取服务监听地址。
     *
     * <p>若服务已绑定，优先返回真实绑定地址。</p>
     */
    public InetSocketAddress getAddress() {
        Channel current = this.serverChannel;
        if (current != null) {
            SocketAddress localAddress = current.localAddress();
            if (localAddress instanceof InetSocketAddress) {
                return (InetSocketAddress) localAddress;
            }
        }
        return this.address;
    }

    /**
     * 获取服务监听端口。
     *
     * <p>当配置端口为 0 时，返回系统分配的实际端口。</p>
     */
    public int getPort() {
        Channel current = this.serverChannel;
        if (current != null) {
            SocketAddress localAddress = current.localAddress();
            if (localAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) localAddress).getPort();
            }
        }
        return this.port;
    }

    /** 获取当前活跃连接 */
    public List<InetSocketAddress> getConnections() {
        List<InetSocketAddress> result = new ArrayList<InetSocketAddress>();
        for (Channel channel : this.connections) {
            SocketAddress remoteAddress = channel.remoteAddress();
            if (remoteAddress instanceof InetSocketAddress) {
                result.add((InetSocketAddress) remoteAddress);
            }
        }
        return result;
    }

    /** 广播文本消息到全部 WebSocket 客户端 */
    public void broadcast(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (Channel channel : this.connections) {
            if (channel.isActive()) {
                channel.writeAndFlush(new TextWebSocketFrame(text));
            }
        }
    }

    /** 从缓存工具读取 server-port */
    private int resolveServerPort() {
        if (!this.forward) {
            return DEFAULT_SERVER_PORT;
        }

        ServerPropertiesTool.refresh();
        String portText = ServerPropertiesTool.getValue("server-port");
        if (isBlank(portText)) {
            this.logger.warn(
                    "forward=true, but server-port is missing in {}. fallback to {}", ServerPropertiesTool.getServerPropertiesPath(), DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }

        final int parsedPort;
        try {
            parsedPort = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            this.logger.warn("server-port is invalid='{}', fallback to {}", portText, DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }
        if (parsedPort <= 0 || parsedPort > 65535) {
            this.logger.warn("server-port out of range='{}', fallback to {}", portText, DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }
        return parsedPort;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String getClientAddress(Channel channel) {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress socketAddress = (InetSocketAddress) remoteAddress;
            return socketAddress.getHostString() + ":" + socketAddress.getPort();
        }
        return String.valueOf(remoteAddress);
    }

    private static void closeChannel(Channel channel, String reason) {
        if (channel != null && channel.isActive()) {
            String closeReason = reason == null ? "" : reason;
            channel.writeAndFlush(new CloseWebSocketFrame(1000, closeReason)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /** 启动失败时回收已创建的 EventLoop 资源，避免线程泄漏。 */
    private void releaseStartResources() {
        if (this.workerGroup != null) {
            this.workerGroup.shutdownGracefully();
            this.workerGroup = null;
        }
        if (this.bossGroup != null) {
            this.bossGroup.shutdownGracefully();
            this.bossGroup = null;
        }
        this.serverChannel = null;
    }

    private static final class ProtocolSwitchHandler extends ByteToMessageDecoder {

        private static final String[] HTTP_METHODS = new String[]{"GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE ", "CONNECT "};

        private final WsServer wsServer;

        private ProtocolSwitchHandler(WsServer wsServer) {
            this.wsServer = wsServer;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (!in.isReadable()) {
                return;
            }

            int readable = Math.min(in.readableBytes(), MAX_DETECT_BYTES);
            String preview = in.toString(in.readerIndex(), readable, CharsetUtil.US_ASCII);

            if (!startsWithHttpMethod(preview)) {
                routeToNonWebsocket(ctx, in, out);
                return;
            }

            boolean hasFullHttpHeader = preview.contains("\r\n\r\n");
            if (!hasFullHttpHeader && in.readableBytes() < MAX_DETECT_BYTES) {
                return;
            }

            if (containsIgnoreCase(preview, "upgrade: websocket")) {
                routeToWebsocket(ctx, in, out);
                return;
            }

            routeToNonWebsocket(ctx, in, out);
        }

        private void routeToWebsocket(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            ctx.pipeline().addLast(new HttpServerCodec());
            ctx.pipeline().addLast(new HttpObjectAggregator(65536));
            ctx.pipeline().addLast(new WebSocketAuthHandler(this.wsServer));
            ctx.pipeline().addLast(new WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true, 65536, false, true));
            ctx.pipeline().addLast(new WebSocketFrameHandler(this.wsServer));
            ctx.pipeline().remove(this);
            out.add(in.readRetainedSlice(in.readableBytes()));
        }

        private void routeToNonWebsocket(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (!this.wsServer.forward) {
                in.skipBytes(in.readableBytes());
                ctx.close();
                return;
            }

            TcpProxyFrontendHandler proxyHandler = new TcpProxyFrontendHandler(LOCALHOST, this.wsServer.proxyTargetPort, this.wsServer.logger);
            ctx.pipeline().addLast(proxyHandler);
            ctx.pipeline().remove(this);
            proxyHandler.connectBackend(ctx);
            out.add(in.readRetainedSlice(in.readableBytes()));
        }

        private static boolean startsWithHttpMethod(String preview) {
            for (String method : HTTP_METHODS) {
                if (preview.regionMatches(true, 0, method, 0, method.length())) {
                    return true;
                }
            }
            return false;
        }

        private static boolean containsIgnoreCase(String text, String token) {
            int max = text.length() - token.length();
            for (int i = 0; i <= max; i++) {
                if (text.regionMatches(true, i, token, 0, token.length())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class WebSocketAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final WsServer wsServer;

        private WebSocketAuthHandler(WsServer wsServer) {
            this.wsServer = wsServer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String upgrade = request.headers().get(HttpHeaderNames.UPGRADE);
            if (upgrade == null || !HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgrade)) {
                reject(ctx, HttpResponseStatus.BAD_REQUEST, "Not websocket request");
                return;
            }

            if (!normalizeWebSocketPath(request)) {
                reject(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid websocket uri");
                return;
            }

            String originServerName = getHeaderOrQueryParam(request, "x-self-name");
            if (originServerName.isEmpty()) {
                this.wsServer.logger.warn(
                        WebsocketConstantMessage.Server.MISSING_SERVER_NAME_HEADER, this.wsServer.getClientAddress(ctx.channel()));
                reject(ctx, HttpResponseStatus.FORBIDDEN, "Missing X-Self-name Header");
                return;
            }

            String clientOrigin = getHeaderOrQueryParam(request, "x-client-origin");
            if ("minecraft".equalsIgnoreCase(clientOrigin)) {
                this.wsServer.logger.warn(
                        WebsocketConstantMessage.Server.INVALID_CLIENT_ORIGIN_HEADER, this.wsServer.getClientAddress(ctx.channel()));
                reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Client-Origin Header cannot be minecraft");
                return;
            }

            String decodedServerName;
            try {
                decodedServerName = URLDecoder.decode(originServerName, StandardCharsets.UTF_8.name());
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                this.wsServer.logger.error(
                        WebsocketConstantMessage.Server.SERVER_NAME_DECODE_FAILED_HEADER, this.wsServer.getClientAddress(ctx.channel()), originServerName, e.getMessage());
                reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header decode failed");
                return;
            }

            if (decodedServerName.isEmpty()) {
                this.wsServer.logger.warn(
                        WebsocketConstantMessage.Server.SERVER_NAME_PARSE_FAILED_HEADER, this.wsServer.getClientAddress(ctx.channel()));
                reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header cannot be empty");
                return;
            }

            if (!decodedServerName.equals(this.wsServer.serverName)) {
                this.wsServer.logger.warn(
                        WebsocketConstantMessage.Server.INVALID_SERVER_NAME_HEADER, this.wsServer.getClientAddress(ctx.channel()), decodedServerName);
                reject(ctx, HttpResponseStatus.FORBIDDEN, "X-Self-name Header is wrong");
                return;
            }

            String authorization = getHeaderOrQueryParam(request, "Authorization");
            if (!this.wsServer.accessToken.isEmpty() && !("Bearer " + this.wsServer.accessToken).equals(authorization)) {
                this.wsServer.logger.warn(
                        WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER, this.wsServer.getClientAddress(ctx.channel()), authorization);
                reject(ctx, HttpResponseStatus.FORBIDDEN, "Authorization Header is wrong");
                return;
            }

            ctx.channel().attr(AUTH_PASSED).set(Boolean.TRUE);
            ctx.fireChannelRead(request.retain());
        }

        private static boolean normalizeWebSocketPath(FullHttpRequest request) {
            try {
                QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
                String rawQuery = decoder.rawQuery();
                String normalizedUri = rawQuery == null || rawQuery.isEmpty() ? WEBSOCKET_PATH : WEBSOCKET_PATH + "?" + rawQuery;
                request.setUri(normalizedUri);
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }

        private String getHeaderOrQueryParam(FullHttpRequest request, String name) {
            String headerValue = request.headers().get(name);
            if (headerValue != null && !headerValue.isEmpty()) {
                return headerValue;
            }

            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
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

    private static final class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

        private final WsServer wsServer;

        private WebSocketFrameHandler(WsServer wsServer) {
            this.wsServer = wsServer;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
            if (!this.wsServer.enabled) {
                return;
            }

            String response = this.wsServer.handleProtocolMessage.handleWebsocketJson(
                    this.wsServer.getClientAddress(ctx.channel()), frame.text());
            if (response != null && !response.isEmpty()) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE && Boolean.TRUE.equals(ctx.channel().attr(AUTH_PASSED).get())) {
                this.wsServer.connections.add(ctx.channel());
                this.wsServer.logger.info(
                        WebsocketConstantMessage.Server.CLIENT_CONNECTED, this.wsServer.getClientAddress(ctx.channel()));
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (this.wsServer.connections.remove(ctx.channel())) {
                this.wsServer.logger.info(
                        WebsocketConstantMessage.Server.CLIENT_DISCONNECTED, this.wsServer.getClientAddress(ctx.channel()));
            }
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            String errorMessage = cause == null || cause.getMessage() == null ? "unknown" : cause.getMessage();
            this.wsServer.logger.warn(
                    WebsocketConstantMessage.Server.CONNECTION_ERROR, this.wsServer.getClientAddress(ctx.channel()), errorMessage);
            ctx.close();
        }
    }

    private static final class TcpProxyFrontendHandler extends ChannelInboundHandlerAdapter {

        private final String backendHost;
        private final int backendPort;
        private final Logger logger;

        private final Queue<Object> pendingWrites = new ArrayDeque<Object>();
        private volatile Channel outboundChannel;
        private volatile boolean connecting;

        private TcpProxyFrontendHandler(String backendHost, int backendPort, Logger logger) {
            this.backendHost = backendHost;
            this.backendPort = backendPort;
            this.logger = logger;
        }

        private void connectBackend(ChannelHandlerContext ctx) {
            if (this.connecting || (this.outboundChannel != null && this.outboundChannel.isActive())) {
                return;
            }
            this.connecting = true;

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new TcpProxyBackendHandler(ctx.channel()));
                }
            });

            bootstrap.connect(this.backendHost, this.backendPort).addListener((ChannelFutureListener) future -> {
                this.connecting = false;
                if (!future.isSuccess()) {
                    this.logger.warn(
                            "failed to forward non-websocket traffic to {}:{}: {}", this.backendHost, this.backendPort, future.cause() == null ? "unknown" : future.cause().getMessage());
                    releasePendingWrites();
                    ctx.close();
                    return;
                }

                this.outboundChannel = future.channel();
                flushPendingWrites();
            });
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (this.outboundChannel != null && this.outboundChannel.isActive()) {
                this.outboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        closeOnFlush(future.channel());
                    }
                });
                return;
            }

            this.pendingWrites.add(msg);
            connectBackend(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            releasePendingWrites();
            closeOnFlush(this.outboundChannel);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            this.logger.warn("proxy frontend error: {}", cause.getMessage());
            releasePendingWrites();
            closeOnFlush(this.outboundChannel);
            ctx.close();
        }

        private void flushPendingWrites() {
            if (this.outboundChannel == null || !this.outboundChannel.isActive()) {
                return;
            }
            Object msg;
            while ((msg = this.pendingWrites.poll()) != null) {
                this.outboundChannel.write(msg);
            }
            this.outboundChannel.flush();
        }

        private void releasePendingWrites() {
            Object msg;
            while ((msg = this.pendingWrites.poll()) != null) {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private static final class TcpProxyBackendHandler extends ChannelInboundHandlerAdapter {

        private final Channel inboundChannel;

        private TcpProxyBackendHandler(Channel inboundChannel) {
            this.inboundChannel = inboundChannel;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            this.inboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    closeOnFlush(future.channel());
                }
            });
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            closeOnFlush(this.inboundChannel);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            closeOnFlush(this.inboundChannel);
            closeOnFlush(ctx.channel());
        }
    }

    private static void closeOnFlush(Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}

