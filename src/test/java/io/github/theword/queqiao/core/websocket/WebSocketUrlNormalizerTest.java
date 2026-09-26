package io.github.theword.queqiao.core.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link WebSocketUrlNormalizer} 单元测试
 *
 * <p>回归目标：合法 URI 不等于合法 WebSocket URL。
 * {@code http://} / {@code ftp://} 都能通过 {@code new URI(...)}，
 * 必须在配置加载阶段拦截，而不是拖到 Client connect 阶段才报错。
 */
class WebSocketUrlNormalizerTest {

    @Test
    @DisplayName("trim、去空、去重且保留首次出现顺序")
    void trimsRemovesEmptyAndDeduplicates() {
        List<String> raw = Arrays.asList(
                "  ws://127.0.0.1:8080/ws  ",
                "",
                "   ",
                "ws://127.0.0.1:8080/ws",
                "wss://example.com/ws",
                null,
                "ws://127.0.0.1:8080/ws");

        WebSocketUrlNormalizer.Result result = WebSocketUrlNormalizer.normalize(raw);

        assertEquals(2, result.getAccepted().size(), "应只保留 2 个唯一 URL，实际=" + result.getAccepted());
        assertEquals("ws://127.0.0.1:8080/ws", result.getAccepted().get(0), "应保留首次出现的顺序");
        assertEquals("wss://example.com/ws", result.getAccepted().get(1));
        assertTrue(result.getRejected().isEmpty(), "不应有被拒绝项");
    }

    @Test
    @DisplayName("非 ws/wss scheme 被拒绝")
    void rejectsUnsupportedSchemes() {
        List<String> raw = Arrays.asList(
                "http://127.0.0.1:8080/ws",
                "https://example.com/ws",
                "ftp://example.com",
                "127.0.0.1:8080/ws",
                "ws://ok.example.com/ws");

        WebSocketUrlNormalizer.Result result = WebSocketUrlNormalizer.normalize(raw);

        assertEquals(1, result.getAccepted().size(), "只应保留 ws:// 一项");
        assertEquals("ws://ok.example.com/ws", result.getAccepted().get(0));
        assertEquals(4, result.getRejected().size(), "其余 4 项都应被拒绝，实际=" + result.getRejected());
    }

    @Test
    @DisplayName("null 列表安全处理")
    void handlesNullList() {
        WebSocketUrlNormalizer.Result result = WebSocketUrlNormalizer.normalize(null);
        assertTrue(result.getAccepted().isEmpty());
        assertTrue(result.getRejected().isEmpty());
    }

    @Test
    @DisplayName("scheme 判断忽略大小写")
    void schemeCheckIsCaseInsensitive() {
        assertTrue(WebSocketUrlNormalizer.hasSupportedScheme("ws://host"));
        assertTrue(WebSocketUrlNormalizer.hasSupportedScheme("wss://host"));
        assertTrue(WebSocketUrlNormalizer.hasSupportedScheme("WS://HOST"));
        assertTrue(WebSocketUrlNormalizer.hasSupportedScheme("WSS://HOST"));
        assertFalse(WebSocketUrlNormalizer.hasSupportedScheme("http://host"));
        assertFalse(WebSocketUrlNormalizer.hasSupportedScheme("ws:/host"));
        assertFalse(WebSocketUrlNormalizer.hasSupportedScheme(null));
    }

    @Test
    @DisplayName("日志脱敏：移除 query string")
    void sanitizeRemovesQueryString() {
        String sanitized = WebSocketUrlNormalizer.sanitizeForLog(
                "ws://127.0.0.1:8080/ws?Authorization=Bearer%20s3cr3t");
        assertEquals("ws://127.0.0.1:8080/ws", sanitized);
        assertFalse(sanitized.contains("s3cr3t"), "脱敏后不得残留 token");
    }

    @Test
    @DisplayName("日志脱敏：遮蔽 user info")
    void sanitizeMasksUserInfo() {
        String sanitized = WebSocketUrlNormalizer.sanitizeForLog("ws://user:password@example.com/ws");
        assertEquals("ws://***@example.com/ws", sanitized);
        assertFalse(sanitized.contains("password"), "脱敏后不得残留凭据");
    }

    @Test
    @DisplayName("日志脱敏：同时含 user info 与 query")
    void sanitizeHandlesBothUserInfoAndQuery() {
        String sanitized = WebSocketUrlNormalizer.sanitizeForLog(
                "wss://user:password@example.com/ws?access_token=abc");
        assertEquals("wss://***@example.com/ws", sanitized);
    }

    @Test
    @DisplayName("日志脱敏：普通 URL 保持不变")
    void sanitizeKeepsPlainUrlUntouched() {
        assertEquals("ws://127.0.0.1:8080/ws", WebSocketUrlNormalizer.sanitizeForLog("ws://127.0.0.1:8080/ws"));
    }

    @Test
    @DisplayName("日志脱敏：null 返回空串")
    void sanitizeHandlesNull() {
        assertEquals("", WebSocketUrlNormalizer.sanitizeForLog(null));
    }
}
