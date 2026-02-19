package com.github.theword.queqiao.tool.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.CharsetUtil;
import java.util.List;

/**
 * 连接入口协议探测处理器。
 *
 * <p>根据首包内容将连接分流为 WebSocket 或 TCP 转发链路。</p>
 */
final class ProtocolSwitchHandler extends ByteToMessageDecoder {

    private static final int MAX_DETECT_BYTES = 8192;
    private static final String LOCALHOST = "127.0.0.1";

    private final WsServer wsServer;

    ProtocolSwitchHandler(WsServer wsServer) {
        this.wsServer = wsServer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) {
            return;
        }

        int readable = Math.min(in.readableBytes(), MAX_DETECT_BYTES);
        String preview = in.toString(in.readerIndex(), readable, CharsetUtil.US_ASCII);

        if (!HttpDetection.isHttpRequest(preview)) {
            routeToNonWebsocket(ctx, in, out);
            return;
        }

        if (!HttpDetection.hasFullHeaders(preview, in.readableBytes(), MAX_DETECT_BYTES)) {
            return;
        }

        if (HttpDetection.isWebSocketUpgrade(preview)) {
            routeToWebsocket(ctx, in, out);
            return;
        }

        routeToNonWebsocket(ctx, in, out);
    }

    /**
     * 将连接切换到 WebSocket 处理链。
     */
    private void routeToWebsocket(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        ctx.pipeline().addLast(new HttpServerCodec());
        ctx.pipeline().addLast(new HttpObjectAggregator(65536));
        ctx.pipeline().addLast(new WebSocketAuthHandler(this.wsServer));
        ctx.pipeline().addLast(new WebSocketServerProtocolHandler(this.wsServer.getWebsocketPath(), null, true, 65536, false, true));
        ctx.pipeline().addLast(new WebSocketFrameHandler(this.wsServer));
        ctx.pipeline().remove(this);
        out.add(in.readRetainedSlice(in.readableBytes()));
    }

    /**
     * 将连接切换到普通 TCP 转发链。
     */
    private void routeToNonWebsocket(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!this.wsServer.isForwardEnabled()) {
            in.skipBytes(in.readableBytes());
            ctx.close();
            return;
        }

        TcpProxyFrontendHandler proxyHandler = new TcpProxyFrontendHandler(
                LOCALHOST, this.wsServer.getProxyTargetPort(), this.wsServer.getInternalLogger()
        );
        ctx.pipeline().addLast(proxyHandler);
        ctx.pipeline().remove(this);
        proxyHandler.connectBackend(ctx);
        out.add(in.readRetainedSlice(in.readableBytes()));
    }
}
