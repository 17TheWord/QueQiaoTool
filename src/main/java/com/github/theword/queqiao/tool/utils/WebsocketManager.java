package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.WebSocketClientConfig;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.websocket.ReconnectPolicy;
import com.github.theword.queqiao.tool.websocket.WebSocketUrlNormalizer;
import com.github.theword.queqiao.tool.websocket.WsClient;
import com.github.theword.queqiao.tool.websocket.WsServer;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 生命周期管理器
 *
 * <p>所有权模型（单向、不允许跨层 shutdown）：
 * <pre>
 * WebsocketManager
 *     owns  →  reconnectScheduler（共享重连调度器）
 * WsClient
 *     owns  →  reconnectFuture / generation / retry state / reconnectInProgress
 * Java-WebSocket
 *     owns  →  socket / read thread / write thread / connectionLostChecker
 * </pre>
 *
 * <p>调度器在构造阶段创建（{@link ScheduledThreadPoolExecutor} 的 core 线程在首次提交任务时才启动，
 * 因此即使未启用 Client 也无实际线程开销），只在 {@link #stop(int, String, Object)} 这一永久销毁路径
 * 关闭；{@code stopClients()} / {@code restartClients()} 绝不触碰它。
 *
 * @since 0.6.11
 */
public class WebsocketManager {

    /**
     * 共享重连调度器核心线程数
     *
     * <p>取 2 而非 1：{@code WebSocketClient.reconnect()} 内部会走 {@code reset() → closeBlocking()}，
     * 而 {@code closeBlocking()} 没有超时。单 worker 会被一个卡住的 Client 拖住，
     * 导致其它 Client 的重连全部停滞。
     */
    private static final int RECONNECT_SCHEDULER_CORE_POOL_SIZE = 2;

    /**
     * 正常关闭使用的关闭码
     */
    private static final int CLOSE_CODE_NORMAL = 1000;

    /**
     * 重连调度器线程名前缀
     */
    private static final String RECONNECT_THREAD_NAME_PREFIX = "QueQiao-WebSocket-Reconnect-";

    private final Object lifecycleLock = new Object();
    private final List<WsClient> wsClientList;
    private volatile WsServer wsServer;
    private final Logger logger;
    private final Gson gson;
    private final HandleCommandReturnMessageService handleCommandReturnMessageService;

    /**
     * 协议分发入口，由 QueQiaoRuntime 创建并注入
     *
     * <p>所有 Client 与 Server 共用同一实例：该对象构造后不可变、无每请求状态，
     * 可安全并发使用。详见 {@link HandleProtocolMessage}。
     */
    private final HandleProtocolMessage handleProtocolMessage;

    /**
     * 共享重连调度器：本 Manager 独占持有，Client 只使用不销毁
     */
    private final ScheduledThreadPoolExecutor reconnectScheduler;

    /**
     * 是否已启动（保证 {@link #start(Object)} 幂等）
     */
    private boolean started = false;

    /**
     * 是否已永久销毁（保证 {@link #stop(int, String, Object)} 幂等）
     */
    private boolean destroyed = false;

    /**
     * 构造 WebSocket 生命周期管理器
     *
     * @param logger                          日志实现
     * @param gson                            Gson 实例
     * @param handleCommandReturnMessageService 命令返回消息实现
     * @param handleProtocolMessage           协议分发入口（由 QueQiaoRuntime 创建并注入）
     */
    public WebsocketManager(
                    Logger logger,
                    Gson gson,
                    HandleCommandReturnMessageService handleCommandReturnMessageService,
                    HandleProtocolMessage handleProtocolMessage) {
        this.logger = logger;
        this.gson = gson;
        this.handleCommandReturnMessageService = handleCommandReturnMessageService;
        this.handleProtocolMessage = handleProtocolMessage;
        this.wsClientList = new ArrayList<>();
        this.reconnectScheduler = createReconnectScheduler();
    }

    /**
     * 创建共享重连调度器
     *
     * @return 已配置好的调度器
     */
    private static ScheduledThreadPoolExecutor createReconnectScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                RECONNECT_SCHEDULER_CORE_POOL_SIZE, new ReconnectThreadFactory());
        // 取消的任务立即从延迟队列移除，避免已取消任务继续占位
        executor.setRemoveOnCancelPolicy(true);
        // 关闭时不再执行已排队的延迟任务
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }

    public List<WsClient> getWsClientList() {
        synchronized (lifecycleLock) {
            return new ArrayList<>(wsClientList);
        }
    }

    public WsServer getWsServer() {
        synchronized (lifecycleLock) {
            return wsServer;
        }
    }

    /**
     * 启动所有 endpoint
     *
     * <p>每个 URL 独立启动：单个 endpoint 失败不得阻断其它 endpoint。
     *
     * @param commandReturner 命令执行者，可为 null
     */
    private void startClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);

        WebSocketClientConfig clientConfig = GlobalContext.getConfig().getWebsocketClient();
        WebSocketUrlNormalizer.Result normalized = WebSocketUrlNormalizer.normalize(clientConfig.getUrlList());

        for (String rejectedUrl : normalized.getRejected()) {
            this.logger.warn("WebSocket URL scheme 不受支持（仅支持 ws:// 与 wss://），已跳过：{}", rejectedUrl);
            this.handleCommandReturnMessageService.sendReturnMessage(
                    commandReturner, buildUriErrorMessage(rejectedUrl));
        }

        ReconnectPolicy reconnectPolicy = new ReconnectPolicy(
                clientConfig.getReconnectInterval(), clientConfig.getReconnectMaxTimes());
        if (!reconnectPolicy.isAutoReconnectEnabled()) {
            this.logger.warn("WebSocket 自动重连未启用（reconnect_max_times <= 0），连接断开后不会自动重连");
        }

        for (String websocketUrl : normalized.getAccepted()) {
            startClient(websocketUrl, reconnectPolicy, commandReturner);
        }
    }

    /**
     * 启动单个 endpoint
     *
     * @param websocketUrl    已归一化的 URL
     * @param reconnectPolicy 退避策略
     * @param commandReturner 命令执行者，可为 null
     */
    private void startClient(String websocketUrl, ReconnectPolicy reconnectPolicy, Object commandReturner) {
        WsClient wsClient = null;
        try {
            URI uri = new URI(websocketUrl);
            wsClient = new WsClient(
                    uri,
                    this.logger,
                    this.reconnectScheduler,
                    reconnectPolicy,
                    this.handleProtocolMessage,
                    GlobalContext.getConfig().getServerName(),
                    GlobalContext.getConfig().getAccessToken(),
                    GlobalContext.getConfig().isEnable()
            );
            // 先纳入管理列表：保证 connect() 同步抛异常时该实例仍能被回收，Manager 始终拥有 Client 生命周期
            this.wsClientList.add(wsClient);
            wsClient.connect();
        } catch (URISyntaxException e) {
            discardFailedClient(wsClient);
            this.logger.warn("WebSocket URL 格式错误，无法连接：{}", WebSocketUrlNormalizer.sanitizeForLog(websocketUrl));
            this.handleCommandReturnMessageService.sendReturnMessage(
                    commandReturner, buildUriErrorMessage(websocketUrl));
        } catch (RuntimeException e) {
            discardFailedClient(wsClient);
            this.logger.warn(
                    "WebSocket 客户端启动失败，url={}，error={}", WebSocketUrlNormalizer.sanitizeForLog(websocketUrl), e.getMessage());
            this.handleCommandReturnMessageService.sendReturnMessage(
                    commandReturner, buildUriErrorMessage(websocketUrl));
        }
    }

    /**
     * 回收启动失败的 Client
     *
     * @param wsClient 可能为 null
     */
    private void discardFailedClient(WsClient wsClient) {
        if (wsClient == null) {
            return;
        }
        this.wsClientList.remove(wsClient);
        try {
            wsClient.stopWithoutReconnect(CLOSE_CODE_NORMAL, WebsocketConstantMessage.CLOSE_BY_RELOAD);
        } catch (RuntimeException e) {
            Tool.debugLog("清理启动失败的 WebSocket 客户端时出现异常：{}", e.getMessage());
        }
    }

    /**
     * 构造 URL 错误提示（已脱敏）
     *
     * @param websocketUrl 原始 URL
     * @return 可直接返回给命令执行者的文本
     */
    private static String buildUriErrorMessage(String websocketUrl) {
        String sanitizedUrl = WebSocketUrlNormalizer.sanitizeForLog(websocketUrl);
        return String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR.replace("{}", "%s"), sanitizedUrl);
    }

    /**
     * 停止所有 Client
     *
     * <p>只停止 Client，<b>绝不</b>关闭共享调度器。
     *
     * @param code            关闭码
     * @param reason          关闭原因模板
     * @param commandReturner 命令执行者，可为 null
     */
    private void stopClients(int code, String reason, Object commandReturner) {
        for (WsClient wsClient : wsClientList) {
            String closeReason = String.format(reason, wsClient.getURI());
            this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, closeReason);
            wsClient.stopWithoutReconnect(code, closeReason);
        }
        wsClientList.clear();
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }

    private void restartClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        stopClients(CLOSE_CODE_NORMAL, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            startClients(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADED);
    }

    private void startServer(Object commandReturner) {
        wsServer = new WsServer(
                new InetSocketAddress(
                        GlobalContext.getConfig().getWebsocketServer().getHost(),
                        GlobalContext.getConfig().getWebsocketServer().getPort()
                ),
                logger,
                handleProtocolMessage,
                GlobalContext.getConfig().getServerName(),
                GlobalContext.getConfig().getAccessToken(),
                GlobalContext.getConfig().isEnable()
        );
        wsServer.start();
        this.handleCommandReturnMessageService.sendReturnMessage(
                commandReturner,
                String.format(
                        WebsocketConstantMessage.Server.SERVER_STARTING.replace("{}", "%s"),
                        GlobalContext.getConfig().getWebsocketServer().getHost(),
                        GlobalContext.getConfig().getWebsocketServer().getPort()
                )
        );
    }

    private void stopServer(Object commandReturner, String reason) {
        if (wsServer != null) {
            try {
                wsServer.stop(0, reason);
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, reason);
            } catch (InterruptedException e) {
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                Tool.debugLog(e.getMessage());
            }
            wsServer = null;
        }
    }

    private void restartServer(Object commandReturner) {
        stopServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            startServer(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.RELOADED);
    }

    /**
     * 启动（幂等）
     *
     * <p>重复调用不会创建第二套 Client，也不会创建第二个调度器。
     *
     * @param commandReturner 命令执行者，可为 null
     */
    public void start(Object commandReturner) {
        synchronized (lifecycleLock) {
            if (destroyed) {
                Tool.debugLog("WebsocketManager 已销毁，忽略启动请求");
                return;
            }
            if (started) {
                Tool.debugLog("WebsocketManager 已启动，忽略重复启动");
                return;
            }
            started = true;
            if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
                startClients(commandReturner);
            }
            if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
                startServer(commandReturner);
            }
        }
    }

    /**
     * 永久停止（幂等）
     *
     * <p>这是唯一会销毁共享调度器的路径。
     *
     * @param code            关闭码
     * @param reason          关闭原因
     * @param commandReturner 命令执行者，可为 null
     */
    public void stop(int code, String reason, Object commandReturner) {
        synchronized (lifecycleLock) {
            if (destroyed) {
                Tool.debugLog("WebsocketManager 已销毁，忽略重复停止");
                return;
            }
            destroyed = true;
            started = false;
            stopClients(code, reason, commandReturner);
            stopServer(commandReturner, reason);
            shutdownReconnectScheduler();
        }
    }

    /**
     * 重载（保留共享调度器）
     *
     * <p>不允许 stop scheduler 后再复用已关闭的 scheduler。
     *
     * @param commandReturner 命令执行者，可为 null
     */
    public void restart(Object commandReturner) {
        synchronized (lifecycleLock) {
            if (destroyed) {
                Tool.debugLog("WebsocketManager 已销毁，忽略重载请求");
                return;
            }
            restartClients(commandReturner);
            restartServer(commandReturner);
            started = true;
        }
    }

    /**
     * 关闭共享重连调度器
     */
    private void shutdownReconnectScheduler() {
        if (this.reconnectScheduler.isShutdown()) {
            return;
        }
        this.reconnectScheduler.shutdownNow();
        Tool.debugLog("WebsocketManager 共享重连调度器已关闭");
    }

    public void sendEvent(BaseEvent event) {
        if (!GlobalContext.getConfig().isEnable()) {
            return;
        }

        String json = gson.toJson(event);
        List<WsClient> wsClientSnapshot;
        WsServer wsServerSnapshot;
        synchronized (lifecycleLock) {
            wsClientSnapshot = new ArrayList<>(wsClientList);
            wsServerSnapshot = wsServer;
        }

        wsClientSnapshot.forEach(wsClient -> sendClientEvent(wsClient, json));
        if (wsServerSnapshot != null) {
            broadcastServerEvent(wsServerSnapshot, json);
        }
    }

    private void sendClientEvent(WsClient wsClient, String json) {
        try {
            if (wsClient.isOpen()) {
                wsClient.send(json);
                Tool.debugLog("WebSocket Client {} send message {}", wsClient.getURI(), json);
            } else {
                Tool.debugLog("WebSocket Client {} is not connected, skip message {}", wsClient.getURI(), json);
            }
        } catch (RuntimeException e) {
            logger.warn("WebSocket Client send failed, uri={}, error={}", wsClient.getURI(), e.getMessage());
        }
    }

    private void broadcastServerEvent(WsServer server, String json) {
        try {
            server.broadcast(json);
            Tool.debugLog("WebSocket Server broadcast message: {}", json);
        } catch (RuntimeException e) {
            logger.warn("WebSocket Server broadcast failed, error={}", e.getMessage());
        }
    }

    /**
     * 重连调度器线程工厂
     *
     * <p>daemon 只作为 JVM 退出时的兜底，不代替显式 shutdown。
     */
    private static final class ReconnectThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, RECONNECT_THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
