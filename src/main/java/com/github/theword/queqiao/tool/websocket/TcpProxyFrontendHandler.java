package com.github.theword.queqiao.tool.websocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Queue;
import org.slf4j.Logger;

/**
 * TCP 转发前端处理器。
 *
 * <p>负责接收客户端入站数据并转发到后端目标端口。</p>
 */
final class TcpProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private final String backendHost;
    private final int backendPort;
    private final Logger logger;

    private final Queue<Object> pendingWrites = new ArrayDeque<Object>();
    private volatile Channel outboundChannel;
    private volatile boolean connecting;

    TcpProxyFrontendHandler(String backendHost, int backendPort, Logger logger) {
        this.backendHost = backendHost;
        this.backendPort = backendPort;
        this.logger = logger;
    }

    void connectBackend(ChannelHandlerContext ctx) {
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
                        "failed to forward non-websocket traffic to {}:{}: {}", this.backendHost, this.backendPort, future.cause() == null ? "unknown" : future.cause().getMessage()
                );
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
                    NettyChannelUtils.closeOnFlush(future.channel());
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
        NettyChannelUtils.closeOnFlush(this.outboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String errorMessage = cause == null || cause.getMessage() == null ? "unknown" : cause.getMessage();
        this.logger.warn("proxy frontend error: {}", errorMessage);
        releasePendingWrites();
        NettyChannelUtils.closeOnFlush(this.outboundChannel);
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
