package io.github.theword.queqiao.core.websocket;

import io.github.theword.queqiao.core.handle.HandleProtocolMessage;
import io.github.theword.queqiao.core.support.PlatformStubs;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsClient 重连生命周期集成测试
 *
 * <p>用真实 socket 而非 mock：{@code WsClient} 的 connect/reconnect/reset/closeBlocking
 * 强依赖真实线程与网络，mock 库内部行为无法覆盖本批修复的缺陷。
 *
 * <p>核心回归用例：<b>endpoint 未监听（ECONNREFUSED）时仍能自动重连</b>。
 * 修复前 {@code onClose} 中的 {@code if (remote && !stopped)} 判定为 false，
 * 自动重连完全依赖 {@code onError} 兜底；一旦调整 onError 语义就会彻底失效。
 */
class WsClientReconnectIntegrationTest {

    static {
        // 必须在 LoggerFactory 初始化前设置，避免测试输出被大量 warn 淹没
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WsClientReconnectIntegrationTest.class);
    private static final Gson GSON = new Gson();

    /**
     * 与生产一致：所有 Client 共用同一个协议分发入口
     */
    private static final HandleProtocolMessage HANDLE_PROTOCOL_MESSAGE = PlatformStubs.newDispatcher(LOGGER, GSON);

    private static final String RECONNECT_THREAD_PREFIX = "QueQiao-WebSocket-Reconnect-";

    /**
     * 轮询等待条件成立
     *
     * <p>使用带截止时间的轮询而非裸 {@code Thread.sleep} + 断言：
     * 条件单调成立时不会偶发失败，且失败时能给出明确超时信息。
     */
    private static void awaitCondition(BooleanSupplier condition, long timeoutMillis, String description) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new AssertionError("等待超时（" + timeoutMillis + "ms）：" + description);
    }

    /**
     * 获取一个当前空闲的本地端口
     *
     * @return 端口号
     */
    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    /**
     * 创建与 WebsocketManager 配置一致的共享调度器
     */
    private static ScheduledThreadPoolExecutor newSharedScheduler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, RECONNECT_THREAD_PREFIX + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return executor;
    }

    private static LatchedWsClient newClient(int port, ScheduledThreadPoolExecutor scheduler, int maxAttempts) throws Exception {
        URI uri = new URI("ws://127.0.0.1:" + port + "/minecraft/ws");
        return new LatchedWsClient(
                uri, LOGGER, scheduler, new ReconnectPolicy(1, maxAttempts), HANDLE_PROTOCOL_MESSAGE, "Server", "", true);
    }

    /**
     * 记录成功连接次数的 WsClient
     *
     * <p>用 {@link CountDownLatch} 取代轮询，避免"连上了但测试还没看到"的偶发失败。
     */
    private static final class LatchedWsClient extends WsClient {

        private final CountDownLatch openedLatch = new CountDownLatch(1);

        private LatchedWsClient(URI uri, Logger logger, ScheduledThreadPoolExecutor scheduler, ReconnectPolicy policy, HandleProtocolMessage handleProtocolMessage, String serverName, String accessToken, boolean enabled) {
            super(uri, logger, scheduler, policy, handleProtocolMessage, serverName, accessToken, enabled,
                    PlatformStubs.newRuntimeUtils(logger));
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            super.onOpen(serverHandshake);
            openedLatch.countDown();
        }

        private boolean awaitOpen(long timeoutMillis) throws InterruptedException {
            return openedLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 强制让协议处理阶段抛 {@link NullPointerException} 的 WsClient
     *
     * <p>{@code HandleProtocolMessage.handleWebsocketJson} 会在进入 try 之前先调用
     * {@code webSocket.getRemoteSocketAddress().toString()}；返回 null 即可稳定复现该异常，
     * 用于验证异常被 {@code onMessage} 拦截而不会逃出 callback。
     */
    private static final class ThrowingWsClient extends WsClient {

        private ThrowingWsClient(URI uri, Logger logger, ScheduledThreadPoolExecutor scheduler, ReconnectPolicy policy) {
            super(uri, logger, scheduler, policy, HANDLE_PROTOCOL_MESSAGE, "Server", "", true,
                    PlatformStubs.newRuntimeUtils(logger));
        }

        @Override
        public InetSocketAddress getRemoteSocketAddress() {
            return null;
        }
    }

    /**
     * 接受任意客户端的测试端点（不做任何握手校验）
     */
    private static final class AcceptAllEndpoint extends WebSocketServer {

        private AcceptAllEndpoint(int port) {
            super(new InetSocketAddress("127.0.0.1", port));
            setReuseAddr(true);
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
        }
    }

    /**
     * 统计当前存活的、名称以指定前缀开头的线程
     */
    private static List<Thread> liveThreads(String prefix) {
        List<Thread> threads = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith(prefix)) {
                threads.add(thread);
            }
        }
        return threads;
    }

    /**
     * 核心回归：endpoint 未监听 → 自动重连 → endpoint 上线 → 最终连接成功
     */
    @Test
    @DisplayName("ECONNREFUSED 后仍能自动重连，并在 endpoint 上线后连接成功")
    void reconnectsAfterEndpointBecomesAvailable() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        AcceptAllEndpoint endpoint = null;
        LatchedWsClient client = newClient(port, scheduler, 10);
        try {
            client.connect();

            // 此时端口无人监听：必须已经安排过自动重连
            awaitCondition(
                    () -> client.getReconnectAttempts() >= 1, 15_000L,
                    "endpoint 未监听时应已安排至少一次自动重连");

            // 端点上线，等待客户端自动重连成功
            endpoint = new AcceptAllEndpoint(port);
            endpoint.start();

            assertTrue(client.awaitOpen(20_000L), "endpoint 上线后客户端应自动重连成功");
            assertTrue(client.isOpen(), "客户端应处于已连接状态");
        } finally {
            client.stopWithoutReconnect(1000, "test cleanup");
            if (endpoint != null) {
                endpoint.stop(1000);
            }
            scheduler.shutdownNow();
        }
    }

    /**
     * 停止后绝不再自动重连
     */
    @Test
    @DisplayName("stopWithoutReconnect 后不再安排任何重连")
    void stopWithoutReconnectPreventsFurtherReconnect() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        LatchedWsClient client = newClient(port, scheduler, 50);
        try {
            client.connect();
            awaitCondition(
                    () -> client.getReconnectAttempts() >= 1, 15_000L,
                    "停止前应已安排至少一次自动重连");

            client.stopWithoutReconnect(1000, "test stop");
            int attemptsAtStop = client.getReconnectAttempts();
            assertTrue(client.isStopped(), "客户端应标记为已停止");

            // 观察窗口：退避基数为 1 秒，若仍有 pending 任务会在窗口内触发并推高计数
            Thread.sleep(2500L);
            assertEquals(attemptsAtStop, client.getReconnectAttempts(), "停止后不应再安排重连");
        } finally {
            client.stopWithoutReconnect(1000, "test cleanup");
            scheduler.shutdownNow();
        }
    }

    /**
     * 多客户端隔离：停止 A 不影响 B、C，且不会关闭共享调度器
     */
    @Test
    @DisplayName("停止单个 Client 不影响其它 Client，也不关闭共享调度器")
    void stoppingOneClientDoesNotAffectOthers() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        LatchedWsClient clientA = newClient(port, scheduler, 50);
        LatchedWsClient clientB = newClient(port, scheduler, 50);
        LatchedWsClient clientC = newClient(port, scheduler, 50);
        try {
            clientA.connect();
            clientB.connect();
            clientC.connect();
            awaitCondition(
                    () -> clientA.getReconnectAttempts() >= 1
                            && clientB.getReconnectAttempts() >= 1
                            && clientC.getReconnectAttempts() >= 1,
                    20_000L,
                    "三个 Client 都应安排自动重连");

            clientA.stopWithoutReconnect(1000, "stop A");
            int attemptsOfAAtStop = clientA.getReconnectAttempts();
            int attemptsOfBBefore = clientB.getReconnectAttempts();

            awaitCondition(
                    () -> clientB.getReconnectAttempts() > attemptsOfBBefore, 15_000L,
                    "B 在 A 停止后应继续重连");

            assertEquals(attemptsOfAAtStop, clientA.getReconnectAttempts(), "A 停止后不应再重连");
            assertTrue(clientC.getReconnectAttempts() >= 1, "C 应仍在重连流程中");
            assertFalse(scheduler.isShutdown(), "单个 Client 停止不得关闭共享调度器");
        } finally {
            clientA.stopWithoutReconnect(1000, "cleanup");
            clientB.stopWithoutReconnect(1000, "cleanup");
            clientC.stopWithoutReconnect(1000, "cleanup");
            scheduler.shutdownNow();
        }
    }

    /**
     * 多个 Client 共用同一个调度器，线程数不超过 corePoolSize
     */
    @Test
    @DisplayName("多 Client 共用共享调度器，重连线程数不超过 corePoolSize")
    void sharedSchedulerThreadCountIsBounded() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        List<LatchedWsClient> clients = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                LatchedWsClient client = newClient(port, scheduler, 50);
                clients.add(client);
                client.connect();
            }
            awaitCondition(
                    () -> clients.get(0).getReconnectAttempts() >= 1, 15_000L,
                    "至少一个 Client 应已安排重连");

            // 上面那个条件只能说明"已决定重连"：WsClient 在决定重连时就自增计数，
            // 而"把任务投递给调度器"发生在其后，"线程池真正 start 线程"又更晚。
            // 因此必须按下面三步分开等待，不能拿"计数已自增"当作"调度线程已存在"的证据——
            // 否则在慢机器（CI）上会偶发地枚举不到线程而失败。

            // ① 任务确实被提交到"这个"共享调度器（而不是某个 Client 自建的调度器）
            awaitCondition(
                    () -> scheduler.getTaskCount() >= 1, 15_000L,
                    "重连任务应已提交到共享调度器");
            // ② 调度器确实登记了 worker。注意 ThreadPoolExecutor.addWorker 是
            //    先在 mainLock 内 workers.add(w)、再在锁外 t.start()，
            //    所以这一步只保证"线程已登记"，不保证"线程已启动"。
            awaitCondition(
                    () -> scheduler.getPoolSize() >= 1, 15_000L,
                    "共享调度器应创建调度线程");
            // ③ 线程确实已 start 并可被枚举（第 ② 步之后才执行 t.start()，故必须单独等待）
            awaitCondition(
                    () -> !liveThreads(RECONNECT_THREAD_PREFIX).isEmpty(), 15_000L,
                    "重连调度线程应已启动");

            List<Thread> reconnectThreads = liveThreads(RECONNECT_THREAD_PREFIX);
            assertFalse(reconnectThreads.isEmpty(), "应存在共享重连调度线程");
            // 断言"本调度器"的线程数，而不是全局同名线程数：
            // 同名线程前缀是生产与测试共用的，其它测试类的调度器可能仍在收尾，
            // 全局计数会随执行顺序变化，属于偶发失败源。
            assertTrue(
                    scheduler.getPoolSize() <= 2,
                    "共享调度器的线程数不得超过 corePoolSize=2，实际=" + scheduler.getPoolSize());
            for (Thread thread : reconnectThreads) {
                assertTrue(thread.isDaemon(), "重连调度线程应为 daemon：" + thread.getName());
            }
        } finally {
            for (LatchedWsClient client : clients) {
                client.stopWithoutReconnect(1000, "cleanup");
            }
            scheduler.shutdownNow();
        }
    }

    /**
     * WS-A2 回归：业务层异常不得逃出 WebSocket callback
     *
     * <p>Java-WebSocket 的读循环会捕获 {@code RuntimeException} 并执行
     * {@code closeConnection(ABNORMAL_CLOSE)}，因此一旦异常逃出 {@code onMessage}，
     * 一条异常消息就能把连接打掉并触发无意义的连接抖动。
     * 本用例通过强制协议处理阶段抛 NPE 来验证异常被拦截。
     */
    @Test
    @DisplayName("onMessage 吞掉业务层 RuntimeException，不让其逃出 callback（WS-A2 回归）")
    void onMessageSwallowsBusinessRuntimeException() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        URI uri = new URI("ws://127.0.0.1:" + port + "/minecraft/ws");
        ThrowingWsClient client = new ThrowingWsClient(uri, LOGGER, scheduler, new ReconnectPolicy(1, 5));
        try {
            // 若 onMessage 未做异常隔离，此处会抛出 NullPointerException 导致用例失败
            client.onMessage("{\"api\":\"broadcast\",\"data\":{}}");
        } finally {
            client.stopWithoutReconnect(1000, "cleanup");
            scheduler.shutdownNow();
        }
    }

    /**
     * 畸形消息不得打断已建立的连接
     */
    @Test
    @DisplayName("畸形消息不会断开已建立的连接")
    void malformedMessageDoesNotDropConnection() throws Exception {
        int port = findFreePort();
        ScheduledThreadPoolExecutor scheduler = newSharedScheduler();
        AcceptAllEndpoint endpoint = new AcceptAllEndpoint(port);
        endpoint.start();
        LatchedWsClient client = newClient(port, scheduler, 5);
        try {
            client.connect();
            assertTrue(client.awaitOpen(15_000L), "客户端应连接成功");

            client.onMessage("this-is-not-json");
            client.onMessage("{\"api\":\"unknown_api\",\"data\":{}}");

            awaitCondition(client::isOpen, 5_000L, "畸形消息后连接应保持打开");
        } finally {
            client.stopWithoutReconnect(1000, "cleanup");
            endpoint.stop(1000);
            scheduler.shutdownNow();
        }
    }
}
