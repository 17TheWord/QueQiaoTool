package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

/** Netty WebSocket 客户端。 */
public class WsClient {
    private static final long MAX_RECONNECT_DELAY_SECONDS = 60L;
    private static final int MIN_RECONNECT_INTERVAL_SECONDS = 1;

    private final URI uri;
    private final Logger logger;
    private final boolean enabled;
    private final HandleProtocolMessage handleProtocolMessage;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "queqiao-ws-reconnect");
        thread.setDaemon(true);
        return thread;
    });

    private final int reconnectMaxTimes;
    private final int reconnectInterval;
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);
    private final AtomicBoolean suppressNextReconnect = new AtomicBoolean(false);

    private final HttpHeaders connectHeaders = new DefaultHttpHeaders();

    private volatile boolean stopped = false;
    private volatile EventLoopGroup group;
    private volatile Channel channel;

    public WsClient(
                    URI uri, Logger logger, Gson gson, String serverName, String accessToken, int reconnectMaxTimes, int reconnectInterval, boolean enabled) {
        this.uri = uri;
        this.logger = logger;
        this.reconnectMaxTimes = Math.max(0, reconnectMaxTimes);
        this.reconnectInterval = Math.max(MIN_RECONNECT_INTERVAL_SECONDS, reconnectInterval);
        this.enabled = enabled;
        this.handleProtocolMessage = new HandleProtocolMessage(logger, gson);

        try {
            this.connectHeaders.add(
                    "x-self-name", URLEncoder.encode(serverName, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            this.logger.error("WebSocket 客户端初始化失败，服务器名称编码异常", e);
        }
        this.connectHeaders.add("x-client-origin", "minecraft");
        if (accessToken != null && !accessToken.isEmpty()) {
            this.connectHeaders.add("Authorization", "Bearer " + accessToken);
        }
    }

    public URI getURI() {
        return this.uri;
    }

    public boolean isOpen() {
        Channel current = this.channel;
        return current != null && current.isActive();
    }

    /** 建立连接（首次启动调用） */
    public void connect() {
        this.stopped = false;
        connectInternal();
    }

    public void send(String message) {
        Channel current = this.channel;
        if (current != null && current.isActive() && message != null && !message.isEmpty()) {
            current.writeAndFlush(new TextWebSocketFrame(message));
        }
    }

    /** 立刻触发重连（命令触发） */
    public void reconnectNow() {
        this.logger.info(WebsocketConstantMessage.Client.MANUAL_RECONNECTING, getURI());
        this.reconnectScheduled.set(false);
        this.suppressNextReconnect.set(true);
        this.scheduler.execute(
                () -> {
                    if (this.stopped) {
                        return;
                    }
                    closeCurrentChannel();
                    shutdownGroup();
                    this.reconnectTimes.set(0);
                    connectInternal();
                });
    }

    /** 停止并关闭连接，不再自动重连 */
    public void stopWithoutReconnect(int code, String reason) {
        this.stopped = true;
        this.reconnectScheduled.set(false);
        this.scheduler.shutdownNow();
        Channel current = this.channel;
        this.channel = null;
        if (current != null) {
            String closeReason = reason == null ? "" : reason;
            current.writeAndFlush(new CloseWebSocketFrame(code, closeReason)).addListener(ChannelFutureListener.CLOSE);
        }
        shutdownGroup();
    }

    /** 建立 Netty 客户端连接并初始化 pipeline */
    private synchronized void connectInternal() {
        if (this.stopped || isOpen()) {
            return;
        }

        URI wsUri = normalizeUri(this.uri);
        String scheme = isBlank(wsUri.getScheme()) ? "ws" : wsUri.getScheme();
        String host = wsUri.getHost();
        int port = wsUri.getPort() == -1 ? ("wss".equalsIgnoreCase(scheme) ? 443 : 80) : wsUri.getPort();

        if (isBlank(host)) {
            this.logger.warn("WebSocket URI 不合法，已跳过连接：{}", wsUri);
            return;
        }

        shutdownGroup();

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslContext;
        if (ssl) {
            try {
                sslContext = SslContextBuilder.forClient().build();
            } catch (Exception e) {
                this.logger.warn("初始化 WSS 失败: {}", e.getMessage());
                scheduleReconnect(nextDelay());
                return;
            }
        } else {
            sslContext = null;
        }

        this.group = new NioEventLoopGroup(1);
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                wsUri, WebSocketVersion.V13, null, true, this.connectHeaders);

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                if (sslContext != null) {
                    pipeline.addLast(sslContext.newHandler(ch.alloc(), host, port));
                }
                pipeline.addLast(new HttpClientCodec());
                pipeline.addLast(new HttpObjectAggregator(65536));
                pipeline.addLast(new WebSocketClientProtocolHandler(handshaker, true));
                pipeline.addLast(new ClientFrameHandler(WsClient.this));
            }
        });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                this.logger.warn(
                        WebsocketConstantMessage.Client.CONNECTION_ERROR, getURI(), future.cause() == null ? "unknown" : future.cause().getMessage(), future.cause());
                shutdownGroup();
                scheduleReconnect(nextDelay());
                return;
            }
            this.channel = future.channel();
        });
    }

    /** 指数退避重连，最大 60 秒 */
    private long nextDelay() {
        int exponent = Math.min(this.reconnectTimes.get(), 30);
        return Math.min(this.reconnectInterval * (1L << exponent), MAX_RECONNECT_DELAY_SECONDS);
    }

    private void scheduleReconnect(long delaySeconds) {
        if (this.stopped || this.scheduler.isShutdown()) {
            return;
        }
        if (this.reconnectTimes.get() >= this.reconnectMaxTimes) {
            this.logger.info(WebsocketConstantMessage.Client.MAX_RECONNECT_ATTEMPTS_REACHED, getURI());
            return;
        }
        if (!this.reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        int attempt = this.reconnectTimes.incrementAndGet();
        this.logger.warn(WebsocketConstantMessage.Client.RECONNECTING, getURI(), attempt);

        this.scheduler.schedule(
                () -> {
                    this.reconnectScheduled.set(false);
                    if (!this.stopped) {
                        connectInternal();
                    }
                }, delaySeconds, TimeUnit.SECONDS);
    }

    private void onHandshakeComplete() {
        this.logger.info(WebsocketConstantMessage.Client.CONNECT_SUCCESSFUL, getURI());
        this.reconnectTimes.set(0);
        this.reconnectScheduled.set(false);
    }

    private void onDisconnected(Throwable throwable) {
        this.channel = null;
        shutdownGroup();
        if (this.stopped || this.suppressNextReconnect.getAndSet(false) || this.reconnectScheduled.get()) {
            return;
        }
        String msg = throwable == null ? "channel inactive" : throwable.getMessage();
        this.logger.warn(WebsocketConstantMessage.Client.CONNECTION_ERROR, getURI(), msg, throwable);
        scheduleReconnect(nextDelay());
    }

    private void closeCurrentChannel() {
        Channel current = this.channel;
        this.channel = null;
        if (current != null) {
            current.close();
        }
    }

    private synchronized void shutdownGroup() {
        EventLoopGroup currentGroup = this.group;
        this.group = null;
        if (currentGroup != null && !currentGroup.isShuttingDown()) {
            currentGroup.shutdownGracefully();
        }
    }

    private static URI normalizeUri(URI original) {
        if (isBlank(original.getHost())) {
            return original;
        }

        String path = original.getRawPath();
        if (isBlank(path)) {
            path = "/";
        }

        String scheme = isBlank(original.getScheme()) ? "ws" : original.getScheme();

        try {
            return new URI(
                    scheme, original.getUserInfo(), original.getHost(), original.getPort(), path, original.getRawQuery(), original.getRawFragment());
        } catch (URISyntaxException e) {
            return original;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class ClientFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

        private final WsClient wsClient;

        private ClientFrameHandler(WsClient wsClient) {
            this.wsClient = wsClient;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
            if (!this.wsClient.enabled) {
                return;
            }
            String response = this.wsClient.handleProtocolMessage.handleWebsocketJson(
                    String.valueOf(ctx.channel().remoteAddress()), frame.text());
            if (response != null && !response.isEmpty()) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
                this.wsClient.onHandshakeComplete();
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            this.wsClient.onDisconnected(null);
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            this.wsClient.onDisconnected(cause);
            ctx.close();
        }
    }
}

