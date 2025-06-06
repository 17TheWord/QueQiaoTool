package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.response.Response;
import lombok.Getter;
import lombok.SneakyThrows;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

import static com.github.theword.queqiao.tool.utils.Tool.*;

/**
 * WebSocket 客户端
 */
public class WsClient extends WebSocketClient {
    /**
     * 重连定时器
     */
    @Getter
    private final Timer timer = new Timer();
    /**
     * 处理协议消息
     */
    private final HandleProtocolMessage handleProtocolMessage = new HandleProtocolMessage();
    private int reconnectTimes = 1;

    /**
     * Websocket Client 构造函数
     *
     * @param uri URI
     */
    @SneakyThrows
    public WsClient(URI uri) {
        super(uri);
        addHeader("x-self-name", URLEncoder.encode(config.getServerName(), StandardCharsets.UTF_8.toString()));
        addHeader("x-client-origin", "minecraft");
        if (!config.getAccessToken().isEmpty())
            addHeader("Authorization", "Bearer " + config.getAccessToken());
    }

    /**
     * 连接打开时
     *
     * @param serverHandshake ServerHandshake
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        logger.info(String.format(WebsocketConstantMessage.Client.CONNECT_SUCCESSFUL, getURI()));
        reconnectTimes = 1;
    }

    /**
     * 收到消息时触发
     * 向服务器游戏内公屏发送信息
     */
    @Override
    public void onMessage(String message) {
        if (config.isEnable()) {
            Response response = handleProtocolMessage.handleWebSocketJson(this, message);
            this.send(response.getJson());
        }
    }

    /**
     * 关闭时
     *
     * @param code   关闭码
     * @param reason 关闭信息
     * @param remote 是否关闭
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (remote && reconnectTimes <= config.getWebsocketClient().getReconnectMaxTimes()) {
            reconnectWebsocket();
        }
    }

    /**
     * 重连
     * 延迟一定时间后重连
     */
    public void reconnectWebsocket() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                reconnect();
            }
        };
        timer.schedule(timerTask, config.getWebsocketClient().getReconnectInterval() * 1000L);
    }

    /**
     * 关闭连接且不重连
     *
     * @param code   关闭码
     * @param reason 关闭信息
     */
    public void stopWithoutReconnect(int code, String reason) {
        timer.cancel();
        close(code, reason);
    }

    /**
     * 重连
     */
    @Override
    public void reconnect() {
        debugLog(String.format(WebsocketConstantMessage.Client.RECONNECTING, getURI(), reconnectTimes));
        reconnectTimes++;
        super.reconnect();
        if (reconnectTimes == config.getWebsocketClient().getReconnectMaxTimes() + 1) {
            logger.info(String.format(WebsocketConstantMessage.Client.MAX_RECONNECT_ATTEMPTS_REACHED, getURI()));
        }
    }

    /**
     * 触发异常时
     *
     * @param exception 所有异常
     */
    @Override
    public void onError(Exception exception) {
        logger.warn(String.format(WebsocketConstantMessage.Client.CONNECTION_ERROR, getURI(), exception.getMessage()));
        if (exception instanceof ConnectException && exception.getMessage().equals("Connection refused: connect") && reconnectTimes <= config.getWebsocketClient().getReconnectMaxTimes()) {
            reconnectWebsocket();
        }
    }

    /**
     * 发送消息
     * 如果连接已打开，则发送消息，否则打印错误信息
     *
     * @param text 消息
     */
    @Override
    public void send(String text) {
        if (isOpen()) {
            super.send(text);
            debugLog(String.format(WebsocketConstantMessage.Client.SEND_MESSAGE, getURI(), text));
        } else {
            debugLog(WebsocketConstantMessage.Client.SEND_MESSAGE_FAILED, getURI(), text);
        }
    }
}
