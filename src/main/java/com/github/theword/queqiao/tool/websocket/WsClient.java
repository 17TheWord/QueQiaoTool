package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.response.Response;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * WebSocket 客户端
 */
public class WsClient extends WebSocketClient {

    private final Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 最大重连次数
     */
    private final int reconnectMaxTimes;

    /**
     * 重连间隔（秒）
     */
    private final int reconnectInterval;

    /**
     * 当前重连第 N 次
     */
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);
    private volatile boolean stopped = false;

    public enum ReconnectReason {
        EXCEPTION, REMOTE_CLOSE, MANUAL
    }

    public WsClient(URI uri, Logger logger, String serverName, String accessToken, int reconnectMaxTimes, int reconnectInterval) {
        super(uri);
        this.logger = logger;
        this.reconnectMaxTimes = reconnectMaxTimes;
        this.reconnectInterval = reconnectInterval;
        try {
            addHeader("x-self-name", URLEncoder.encode(serverName, StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        addHeader("x-client-origin", "minecraft");
        if (!accessToken.isEmpty()) {
            addHeader("Authorization", "Bearer " + accessToken);
        }
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        this.logger.info(WebsocketConstantMessage.Client.CONNECT_SUCCESSFUL, getURI());
        reconnectTimes.set(0);
    }

    @Override
    public void onMessage(String message) {
        if (GlobalContext.getConfig().isEnable()) {
            Response response = GlobalContext.getHandleProtocolMessage().handleWebSocketJson(this, message);
            this.send(GsonUtils.buildGson().toJson(response));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.warn(WebsocketConstantMessage.Client.CLOSING_CONNECTION, getURI(), code, reason);
        if (remote) {
            scheduleReconnect(nextDelay());
        }
    }

    @Override
    public void onError(Exception exception) {
        logger.warn(WebsocketConstantMessage.Client.CONNECTION_ERROR, getURI(), exception.getMessage(), exception);
        scheduleReconnect(nextDelay());
    }

    /**
     * 计算下一次重连的延迟时间
     *
     * @return 延迟时间（秒）
     */
    private long nextDelay() {
        return Math.min(reconnectInterval * (1L << reconnectTimes.get()), 60);
    }

    /**
     * 安排重连任务
     */
    private void scheduleReconnect(long delaySeconds) {
        if (stopped || reconnectTimes.get() >= reconnectMaxTimes) {
            logger.info(WebsocketConstantMessage.Client.MAX_RECONNECT_ATTEMPTS_REACHED, getURI());
            return;
        }
        reconnectTimes.incrementAndGet();
        logger.debug(WebsocketConstantMessage.Client.RECONNECTING, getURI(), reconnectTimes);
        scheduler.schedule(() -> {
            if (!stopped) super.reconnect();
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 主动立即重连（适用于 reload 等场景）
     */
    public void reconnectNow() {
        scheduleReconnect(0);
    }

    /**
     * 停止并不再重连
     *
     * @param code   关闭代码
     * @param reason 关闭原因
     */
    public void stopWithoutReconnect(int code, String reason) {
        stopped = true;
        scheduler.shutdownNow();
        close(code, reason);
    }

    @Override
    public void send(String text) {
        if (isOpen()) {
            super.send(text);
            logger.debug(WebsocketConstantMessage.Client.SEND_MESSAGE, getURI(), text);
        } else {
            logger.debug(WebsocketConstantMessage.Client.SEND_MESSAGE_FAILED, getURI(), text);
        }
    }
}


