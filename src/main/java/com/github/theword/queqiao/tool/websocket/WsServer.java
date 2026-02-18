package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.utils.ServerPropertiesTool;
import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Netty WebSocket 服务端。
 *
 * <p>支持两类流量：</p>
 * <p>1. WebSocket：按既有协议鉴权并处理消息</p>
 * <p>2. 非 WebSocket：可按配置转发到本地 MC 主端口</p>
 */
public class WsServer {

    private static final String WEBSOCKET_PATH = "/minecraft/ws";
    private static final int DEFAULT_SERVER_PORT = 25565;

    private final InetSocketAddress address;
    private final int port;
    private final Logger logger;
    private volatile String serverName;
    private volatile String accessToken;
    private volatile boolean enabled;
    private volatile boolean forward;
    private final HandleProtocolMessage handleProtocolMessage;

    /** 当前已完成握手并通过鉴权的 WebSocket 连接 */
    private final Set<Channel> connections = ConcurrentHashMap.newKeySet();

    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel serverChannel;
    private volatile int proxyTargetPort = DEFAULT_SERVER_PORT;

    public WsServer(
                    InetSocketAddress address, Logger logger, Gson gson, String serverName, String accessToken, boolean enabled
    ) {
        this(address, logger, gson, serverName, accessToken, enabled, false);
    }

    public WsServer(
                    InetSocketAddress address, Logger logger, Gson gson, String serverName, String accessToken, boolean enabled, boolean forward
    ) {
        this.address = address;
        this.port = address.getPort();
        this.logger = logger;
        this.serverName = serverName == null ? "" : serverName;
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

        this.bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        this.workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

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
            this.logger.info(WebsocketConstantMessage.Server.SERVER_STARTING, this.address.getHostString(), getPort());
        } catch (RuntimeException e) {
            releaseStartResources();
            throw e;
        }
    }

    /**
     * 当前服务端是否处于活动状态。
     *
     * @return true 表示服务端正在监听
     */
    public boolean isActive() {
        Channel current = this.serverChannel;
        return current != null && current.isActive();
    }

    /**
     * 判断当前实例的监听地址是否与目标地址一致。
     *
     * @param host 目标 host
     * @param port 目标 port
     * @return true 表示可复用当前监听端口
     */
    public boolean hasSameBinding(String host, int port) {
        return this.port == port && normalizeHost(this.address.getHostString()).equals(normalizeHost(host));
    }

    /**
     * 热更新运行时配置（不重启监听）。
     *
     * <p>用于 reload 且监听地址不变场景，避免中断当前 TCP 转发连接。</p>
     *
     * @param serverName  服务器名
     * @param accessToken 访问令牌
     * @param enabled     工具启用状态
     * @param forward     非 WebSocket 流量转发开关
     */
    public synchronized void applyRuntimeConfig(String serverName, String accessToken, boolean enabled, boolean forward) {
        this.serverName = serverName == null ? "" : serverName;
        this.accessToken = accessToken == null ? "" : accessToken;
        this.enabled = enabled;
        this.forward = forward;
        this.proxyTargetPort = forward ? resolveServerPort() : DEFAULT_SERVER_PORT;
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
                channel.writeAndFlush(new io.netty.handler.codec.http.websocketx.TextWebSocketFrame(text));
            }
        }
    }

    String getWebsocketPath() {
        return WEBSOCKET_PATH;
    }

    Logger getInternalLogger() {
        return this.logger;
    }

    boolean isForwardEnabled() {
        return this.forward;
    }

    int getProxyTargetPort() {
        return this.proxyTargetPort;
    }

    String getServerNameValue() {
        return this.serverName;
    }

    String getAccessTokenValue() {
        return this.accessToken;
    }

    boolean isToolEnabled() {
        return this.enabled;
    }

    HandleProtocolMessage getHandleProtocolMessage() {
        return this.handleProtocolMessage;
    }

    String getClientAddress(Channel channel) {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            InetSocketAddress socketAddress = (InetSocketAddress) remoteAddress;
            return socketAddress.getHostString() + ":" + socketAddress.getPort();
        }
        return String.valueOf(remoteAddress);
    }

    boolean addConnection(Channel channel) {
        return this.connections.add(channel);
    }

    boolean removeConnection(Channel channel) {
        return this.connections.remove(channel);
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
                    "forward=true, but server-port is missing in {}. fallback to {}", ServerPropertiesTool.getServerPropertiesPath(), DEFAULT_SERVER_PORT
            );
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

    private static String normalizeHost(String host) {
        return host == null ? "" : host.trim().toLowerCase();
    }

    private static void closeChannel(Channel channel, String reason) {
        if (channel != null && channel.isActive()) {
            String closeReason = reason == null ? "" : reason;
            channel.writeAndFlush(new CloseWebSocketFrame(1000, closeReason)).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
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
}
