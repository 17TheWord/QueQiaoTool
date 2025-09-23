package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.response.Response;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static com.github.theword.queqiao.tool.utils.Tool.debugLog;


public class WsServer extends WebSocketServer {

    private final String hostName;
    private final int port;

    /**
     * 构造函数
     *
     * @param address 地址
     */
    public WsServer(InetSocketAddress address) {
        super(address);
        super.setReuseAddr(true);
        this.hostName = address.getHostName();
        this.port = address.getPort();
    }

    /**
     * 获取客户端地址
     *
     * @param webSocket 客户端
     * @return 客户端地址
     */
    private String getClientAddress(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().toString().replaceFirst("/", "");
    }

    /**
     * 当客户端连接时执行
     * 连接将依次检验 x-self-name；x-client-origin；Authorization字段
     *
     * @param webSocket       客户端
     * @param clientHandshake 客户端握手信息
     */
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        String originServerName = clientHandshake.getFieldValue("x-self-name");
        if (originServerName.isEmpty()) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.MISSING_SERVER_NAME_HEADER, getClientAddress(webSocket));
            webSocket.close(1008, "Missing X-Self-name Header");
            return;
        }

        String clientOrigin = clientHandshake.getFieldValue("x-client-origin");
        if (clientOrigin.equalsIgnoreCase("minecraft")) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.INVALID_CLIENT_ORIGIN_HEADER, getClientAddress(webSocket));
            webSocket.close(1008, "X-Client-Origin Header cannot be minecraft");
            return;
        }

        String serverName;
        try {
            serverName = URLDecoder.decode(originServerName, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            GlobalContext.getLogger().error(WebsocketConstantMessage.Server.SERVER_NAME_DECODE_FAILED_HEADER, getClientAddress(webSocket), originServerName, e.getMessage());
            webSocket.close(1008, "X-Self-name Header decode failed");
            return;
        }

        if (serverName.isEmpty()) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.SERVER_NAME_PARSE_FAILED_HEADER, getClientAddress(webSocket));
            webSocket.close(1008, "X-Self-name Header cannot be empty");
            return;
        }

        if (!serverName.equals(GlobalContext.getConfig().getServerName())) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.INVALID_SERVER_NAME_HEADER, getClientAddress(webSocket), serverName);
            webSocket.close(1008, "X-Self-name Header is wrong");
            return;
        }

        String accessToken = clientHandshake.getFieldValue("Authorization");
        if (!GlobalContext.getConfig().getAccessToken().isEmpty() && !accessToken.equals("Bearer " + GlobalContext.getConfig().getAccessToken())) {
            GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER, getClientAddress(webSocket), accessToken);
            webSocket.close(1008, "Authorization Header is wrong");
            return;
        }

        GlobalContext.getLogger().info(WebsocketConstantMessage.Server.CLIENT_CONNECTED, getClientAddress(webSocket));
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
        GlobalContext.getLogger().info(closeReason, getClientAddress(webSocket));
    }

    /**
     * 当接收到客户端的消息时执行
     *
     * @param webSocket 客户端
     * @param message   消息
     */
    @Override
    public void onMessage(WebSocket webSocket, String message) {
        if (GlobalContext.getConfig().isEnable()) {
            Response response = GlobalContext.getHandleProtocolMessage().handleWebSocketJson(webSocket, message);
            webSocket.send(response.getJson());
        }
    }

    /**
     * 当连接出现异常时执行
     *
     * @param webSocket 客户端
     * @param exception 异常
     */
    @Override
    public void onError(WebSocket webSocket, Exception exception) {
        GlobalContext.getLogger().warn(WebsocketConstantMessage.Server.CONNECTION_ERROR, getClientAddress(webSocket), exception.getMessage());
    }

    /**
     * 当服务器启动时执行
     */
    @Override
    public void onStart() {
        GlobalContext.getLogger().info(WebsocketConstantMessage.Server.SERVER_STARTING, hostName, port);
    }

    /**
     * 广播消息
     * 发送内容在debugLog中
     *
     * @param text 消息
     */
    @Override
    public void broadcast(String text) {
        super.broadcast(text);
        debugLog(WebsocketConstantMessage.Server.BROADCAST_MESSAGE, text);
    }
}
