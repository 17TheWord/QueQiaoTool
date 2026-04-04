package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket 文本帧业务处理器。
 */
final class WebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final WsServer wsServer;

    WebSocketFrameHandler(WsServer wsServer) {
        this.wsServer = wsServer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        if (!this.wsServer.isToolEnabled()) {
            return;
        }

        String response = this.wsServer.getHandleProtocolMessage().handleWebsocketJson(
                this.wsServer.getClientAddress(ctx.channel()), frame.text()
        );
        if (response != null && !response.isEmpty()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete && Boolean.TRUE.equals(ctx.channel().attr(WebSocketAuthHandler.AUTH_PASSED).get())) {
            if (this.wsServer.addConnection(ctx.channel())) {
                this.wsServer.getInternalLogger().info(
                        WebsocketConstantMessage.Server.CLIENT_CONNECTED, this.wsServer.getClientAddress(ctx.channel())
                );
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (this.wsServer.removeConnection(ctx.channel())) {
            this.wsServer.getInternalLogger().info(
                    WebsocketConstantMessage.Server.CLIENT_DISCONNECTED, this.wsServer.getClientAddress(ctx.channel())
            );
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String errorMessage = cause == null || cause.getMessage() == null ? "unknown" : cause.getMessage();
        this.wsServer.getInternalLogger().warn(
                WebsocketConstantMessage.Server.CONNECTION_ERROR, this.wsServer.getClientAddress(ctx.channel()), errorMessage
        );
        ctx.close();
    }
}
