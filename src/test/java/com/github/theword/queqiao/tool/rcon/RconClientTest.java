package com.github.theword.queqiao.tool.rcon;

import org.glavo.rcon.Rcon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RconClient} 单元测试
 *
 * <p><b>A#29 回归</b>：{@code connect()} 在"已连接"分支缺少 {@code return}，
 * 会继续执行 {@code new Rcon(...)} 并用新连接覆盖 {@code client} 字段，
 * 旧连接的 socket 不被关闭 → 泄漏。
 *
 * <p><b>验证方式</b>：{@code org.glavo.rcon.Rcon} 是 {@code final class}，无法 mock；
 * 但 {@code RconClient.client} 是 {@code public volatile} 字段、{@code isConnected()} 只判
 * {@code client != null}。因此可以塞入 {@code new Rcon()}
 * （无参构造器不建立连接）伪造"已连接"状态，
 * 再用一个<b>只计数 accept</b> 的本地端口观察 {@code connect()} 是否仍去建立新连接——
 * 无需实现 RCON 协议。
 */
class RconClientTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RconClientTest.class);

    @Test
    @DisplayName("已连接状态下 connect() 不再建立新连接（A#29 回归）")
    void connectDoesNothingWhenAlreadyConnected() throws Exception {
        try (AcceptCountingServer stub = new AcceptCountingServer()) {
            RconClient rconClient = new RconClient(LOGGER, stub.getPort(), "test-password");

            // 伪造"已连接"状态：Rcon 无参构造器不建立任何连接
            rconClient.client = new Rcon();
            assertTrue(rconClient.isConnected(), "塞入非 null client 后应视为已连接");

            rconClient.connect();

            assertEquals(
                    0,
                    stub.getAcceptedCount(),
                    "已连接状态下 connect() 不得再建立新连接（否则旧 socket 泄漏）");
        }
    }

    @Test
    @DisplayName("未连接时 connect() 会尝试建立连接（确保修复没有把正常路径堵死）")
    void connectAttemptsConnectionWhenNotConnected() throws Exception {
        try (AcceptCountingServer stub = new AcceptCountingServer()) {
            RconClient rconClient = new RconClient(LOGGER, stub.getPort(), "test-password");

            rconClient.connect();

            assertEquals(1, stub.getAcceptedCount(), "未连接状态下应尝试建立一次连接");
        }
    }

    /**
     * 只统计"被连接了几次"的桩服务端
     *
     * <p>accept 后立即关闭连接：不需要实现 RCON 协议——
     * 我们只关心客户端是否发起了 TCP 连接。
     */
    private static final class AcceptCountingServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final AtomicInteger acceptedCount = new AtomicInteger();
        private final Thread acceptThread;

        private AcceptCountingServer() throws IOException {
            this.serverSocket = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            this.acceptThread = new Thread(this::acceptLoop, "rcon-test-accept-loop");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    acceptedCount.incrementAndGet();
                    socket.close();
                } catch (IOException e) {
                    return;
                }
            }
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private int getAcceptedCount() {
            return acceptedCount.get();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
            acceptThread.interrupt();
        }
    }
}
