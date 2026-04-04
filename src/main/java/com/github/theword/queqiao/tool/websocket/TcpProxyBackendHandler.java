package com.github.theword.queqiao.tool.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * TCP 转发后端处理器。
 *
 * <p>负责将后端响应回写给前端连接。</p>
 */
final class TcpProxyBackendHandler extends ChannelInboundHandlerAdapter {

    private final Channel inboundChannel;

    TcpProxyBackendHandler(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        this.inboundChannel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                NettyChannelUtils.closeOnFlush(future.channel());
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        NettyChannelUtils.closeOnFlush(this.inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        NettyChannelUtils.closeOnFlush(this.inboundChannel);
        NettyChannelUtils.closeOnFlush(ctx.channel());
    }
}
