package io.github.theword.queqiao.core.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * WebSocket URL 归一化与日志脱敏
 *
 * <p>纯逻辑组件：无网络、无线程、无全局状态依赖。
 *
 * <p>归一化规则（顺序固定）：
 * <ol>
 *     <li>trim</li>
 *     <li>丢弃空串</li>
 *     <li>校验 scheme，仅接受 {@code ws://} 与 {@code wss://}</li>
 *     <li>去重（保留首次出现的顺序）</li>
 * </ol>
 *
 * <p>为什么必须校验 scheme：合法 URI 不等于合法 WebSocket URL。
 * {@code http://} / {@code ftp://} 都能通过 {@code new URI(...)}，
 * 若不在此处拦截，错误会一直拖到 Client 连接阶段才暴露。
 *
 * @since 0.6.11
 */
public final class WebSocketUrlNormalizer {

    private static final String WS_SCHEME = "ws://";
    private static final String WSS_SCHEME = "wss://";
    private static final String SCHEME_SEPARATOR = "://";
    private static final String MASK = "***";
    private static final char QUERY_START = '?';
    private static final char USER_INFO_END = '@';

    private WebSocketUrlNormalizer() {
    }

    /**
     * 归一化结果
     */
    public static final class Result {

        private final List<String> accepted;
        private final List<String> rejected;

        private Result(List<String> accepted, List<String> rejected) {
            this.accepted = Collections.unmodifiableList(accepted);
            this.rejected = Collections.unmodifiableList(rejected);
        }

        /**
         * 可用的 URL（已 trim、去空、去重、scheme 合法）
         *
         * @return 不可变列表
         */
        public List<String> getAccepted() {
            return accepted;
        }

        /**
         * 因 scheme 非法被拒绝的 URL（已脱敏，可安全写日志）
         *
         * @return 不可变列表
         */
        public List<String> getRejected() {
            return rejected;
        }
    }

    /**
     * 归一化 URL 列表
     *
     * @param rawUrls 原始配置列表，允许为 null
     * @return 归一化结果，永不为 null
     */
    public static Result normalize(List<String> rawUrls) {
        List<String> accepted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (rawUrls == null) {
            return new Result(accepted, rejected);
        }

        Set<String> seen = new LinkedHashSet<>();
        for (String rawUrl : rawUrls) {
            if (rawUrl == null) {
                continue;
            }
            String trimmed = rawUrl.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!hasSupportedScheme(trimmed)) {
                rejected.add(sanitizeForLog(trimmed));
                continue;
            }
            if (seen.add(trimmed)) {
                accepted.add(trimmed);
            }
        }
        return new Result(accepted, rejected);
    }

    /**
     * 判断是否使用了受支持的 scheme
     *
     * @param url 待判断的 URL
     * @return 仅当以 {@code ws://} 或 {@code wss://} 开头（忽略大小写）时为 true
     */
    public static boolean hasSupportedScheme(String url) {
        if (url == null) {
            return false;
        }
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.startsWith(WS_SCHEME) || lowerCaseUrl.startsWith(WSS_SCHEME);
    }

    /**
     * 脱敏 URL 以便安全写入日志
     *
     * <p>移除两处可能承载凭据的内容：
     * <ul>
     *     <li>query string —— 例如 {@code ?Authorization=Bearer%20xxx}</li>
     *     <li>user info —— 例如 {@code ws://user:password@host}</li>
     * </ul>
     *
     * @param url 原始 URL，允许为 null
     * @return 可安全记录的文本
     */
    public static String sanitizeForLog(String url) {
        if (url == null) {
            return "";
        }
        String result = url;
        int queryIndex = result.indexOf(QUERY_START);
        if (queryIndex >= 0) {
            result = result.substring(0, queryIndex);
        }

        int schemeIndex = result.indexOf(SCHEME_SEPARATOR);
        if (schemeIndex < 0) {
            return result;
        }
        int userInfoEnd = result.indexOf(USER_INFO_END, schemeIndex + SCHEME_SEPARATOR.length());
        if (userInfoEnd < 0) {
            return result;
        }
        return result.substring(0, schemeIndex + SCHEME_SEPARATOR.length()) + MASK + result.substring(userInfoEnd);
    }
}
