package com.github.theword.queqiao.tool.websocket;

import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsServer 握手认证测试
 *
 * <p>覆盖 WS-A3（Authorization 日志脱敏）与既有握手校验行为不被破坏。
 */
class WsServerHandshakeAuthTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WsServerHandshakeAuthTest.class);
    private static final Gson GSON = new Gson();

    /**
     * 与生产一致：Server 与 Client 共用同一个协议分发入口
     */
    private static final HandleProtocolMessage HANDLE_PROTOCOL_MESSAGE = new HandleProtocolMessage(LOGGER, GSON);

    private static final String SERVER_NAME = "TestServer";
    private static final String ACCESS_TOKEN = "s3cr3t-token";
    private static final int CLOSE_CODE_POLICY_VIOLATION = 1008;

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        return count;
    }

    /**
     * WS-A3 回归：认证失败日志模板只为"客户端地址"保留占位符，绝不为 token 预留占位符
     *
     * <p>修复前模板为 {@code "连接身份验证码：{} 失败"}，调用方会把客户端提交的
     * Authorization 值原样填进去，导致 token 泄漏到日志。
     */
    @Test
    @DisplayName("认证失败日志模板不含 token 占位符（WS-A3 回归）")
    void invalidAccessTokenLogTemplateHasNoTokenPlaceholder() {
        String template = WebsocketConstantMessage.Server.INVALID_ACCESS_TOKEN_HEADER;

        assertEquals(
                1, countOccurrences(template, "{}"),
                "认证失败日志只应保留客户端地址这一个占位符，不得为 token 预留占位符，实际模板=" + template);
    }

    /**
     * 握手认证行为：错误 token 被拒绝（1008），正确 token 被接受
     */
    @Test
    @DisplayName("错误 token 以 1008 被拒绝，正确 token 被接受")
    void rejectsWrongTokenAndAcceptsCorrectToken() throws Exception {
        int port = findFreePort();
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", port), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, ACCESS_TOKEN, true);
        server.start();

        ProbeClient wrongTokenClient = new ProbeClient(port, SERVER_NAME, "Bearer wrong-token");
        ProbeClient correctTokenClient = new ProbeClient(port, SERVER_NAME, "Bearer " + ACCESS_TOKEN);
        try {
            wrongTokenClient.connect();
            assertTrue(wrongTokenClient.awaitClosed(10_000L), "携带错误 token 的连接应被服务端关闭");
            assertEquals(CLOSE_CODE_POLICY_VIOLATION, wrongTokenClient.getCloseCode(), "应以 1008 关闭");

            correctTokenClient.connect();
            assertTrue(correctTokenClient.awaitOpen(10_000L), "携带正确 token 的连接应被接受");
        } finally {
            wrongTokenClient.close();
            correctTokenClient.close();
            server.stop(1000);
        }
    }

    /**
     * 缺失 x-self-name 的连接被拒绝
     */
    @Test
    @DisplayName("缺失 x-self-name 的连接以 1008 被拒绝")
    void rejectsMissingServerNameHeader() throws Exception {
        int port = findFreePort();
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", port), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, "", true);
        server.start();

        ProbeClient clientWithoutName = new ProbeClient(port, null, null);
        try {
            clientWithoutName.connect();
            assertTrue(clientWithoutName.awaitClosed(10_000L), "缺失 x-self-name 的连接应被服务端关闭");
            assertEquals(CLOSE_CODE_POLICY_VIOLATION, clientWithoutName.getCloseCode(), "应以 1008 关闭");
        } finally {
            clientWithoutName.close();
            server.stop(1000);
        }
    }

    /**
     * C2 回归：`onOpen` 绝不允许异常逃出
     *
     * <p>Java-WebSocket 的 {@code WebSocketImpl.open()} 会吞掉 {@code onOpen} 抛出的
     * {@code RuntimeException} 并让连接保持 {@code OPEN}，导致未鉴权连接仍可用。
     * 因此本方法必须自己兜底——传入会触发内部 NPE 的输入，断言异常不外泄。
     *
     * <p>这里刻意传 {@code (null, null)}：{@code getHeaderOrQueryParam(null, ...)} 会在
     * {@code clientHandshake.getFieldValue(name)} 处抛 NPE。修复前该异常会直接逃出 {@code onOpen}。
     */
    @Test
    @DisplayName("onOpen 绝不让异常逃出（C2 回归）")
    void onOpenNeverLetsExceptionEscape() {
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", 1), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, ACCESS_TOKEN, true);

        server.onOpen(null, null);
    }

    /**
     * C1 回归：服务端级致命错误会以 {@code onError(null, e)} 回调，此时不得 NPE
     */
    @Test
    @DisplayName("onError 收到 null 连接时不抛异常（C1 回归）")
    void onErrorWithNullConnectionIsSafe() {
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", 1), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, ACCESS_TOKEN, true);

        server.onError(null, new IllegalStateException("selector fatal error"));
        // 异常消息为 null 时也应安全
        server.onError(null, new IllegalStateException());
    }

    /**
     * C3 回归：显式设置连接丢失检测周期，不依赖库默认值
     */
    @Test
    @DisplayName("connectionLostTimeout 显式设置为 60 秒（C3 回归）")
    void connectionLostTimeoutIsExplicit() {
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", 1), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, ACCESS_TOKEN, true);

        assertEquals(60, server.getConnectionLostTimeout(), "应显式设置 connectionLostTimeout");
    }

    /**
     * C2 配套：构造器把 null 归一化为空串，避免握手阶段 NPE
     *
     * <p>{@code accessToken} 为空表示不鉴权，因此该连接应被**接受**而不是因 NPE 被保留为异常状态。
     */
    @Test
    @DisplayName("accessToken 为 null 时归一化为不鉴权，连接被正常接受")
    void nullAccessTokenMeansNoAuthRequired() throws Exception {
        int port = findFreePort();
        WsServer server = new WsServer(
                new InetSocketAddress("127.0.0.1", port), LOGGER, HANDLE_PROTOCOL_MESSAGE, SERVER_NAME, null, true);
        server.start();

        ProbeClient client = new ProbeClient(port, SERVER_NAME, null);
        try {
            client.connect();
            assertTrue(client.awaitOpen(10_000L), "不鉴权配置下连接应被接受");

            // 鉴权通过后消息应被正常处理：未知 api 会返回 404 响应
            client.send("{\"api\":\"no_such_api\",\"echo\":\"positive-path\"}");
            assertTrue(client.awaitMessage(10_000L), "已鉴权连接的消息应被处理并返回响应");
            assertTrue(
                    client.getLastMessage().contains("positive-path"),
                    "响应应回传 echo，实际=" + client.getLastMessage());
        } finally {
            client.close();
            server.stop(1000);
        }
    }

    /**
     * 探测用客户端：可自定义 x-self-name 与 Authorization
     */
    private static final class ProbeClient extends WebSocketClient {

        private final CountDownLatch openLatch = new CountDownLatch(1);
        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private final CountDownLatch messageLatch = new CountDownLatch(1);
        private volatile int closeCode = Integer.MIN_VALUE;
        private volatile String lastMessage;

        private ProbeClient(int port, String selfName, String authorization) throws Exception {
            super(new URI("ws://127.0.0.1:" + port + "/minecraft/ws"));
            if (selfName != null) {
                addHeader("x-self-name", selfName);
            }
            addHeader("x-client-origin", "test-probe");
            if (authorization != null) {
                addHeader("Authorization", authorization);
            }
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            openLatch.countDown();
        }

        @Override
        public void onMessage(String message) {
            this.lastMessage = message;
            messageLatch.countDown();
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            this.closeCode = code;
            closeLatch.countDown();
        }

        @Override
        public void onError(Exception exception) {
        }

        private boolean awaitOpen(long timeoutMillis) throws InterruptedException {
            return openLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        private boolean awaitClosed(long timeoutMillis) throws InterruptedException {
            return closeLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        private boolean awaitMessage(long timeoutMillis) throws InterruptedException {
            return messageLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        private int getCloseCode() {
            return closeCode;
        }

        private String getLastMessage() {
            return lastMessage;
        }
    }
}
