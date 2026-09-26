package io.github.theword.queqiao.core.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 日志脱敏工具
 *
 * <p>纯逻辑组件：不依赖网络、线程与全局状态，可独立单元测试。
 *
 * <p>用途：把可能包含敏感信息的 JSON 文本转换为可安全写入日志的形式——
 * <ol>
 *     <li>按字段名脱敏（{@code token} / {@code password} / {@code authorization} 等，递归处理嵌套对象与数组）</li>
 *     <li>按长度截断</li>
 * </ol>
 *
 * <p><b>能力边界</b>：脱敏是"按已知字段名"的尽力而为。字段名不在清单内的敏感内容无法被识别，
 * 因此<b>长度截断是最后一道兜底</b>，不应被移除。
 *
 * <p>本类在任何输入下都不抛异常——日志路径上的工具一旦抛异常会掩盖真正的问题。
 *
 * @since 0.6.11
 */
public final class LogSanitizer {

    /**
     * 默认最大输出长度
     */
    public static final int DEFAULT_MAX_LENGTH = 1024;

    /**
     * 需要脱敏的字段名（小写匹配）
     */
    private static final Set<String> SENSITIVE_FIELD_NAMES = new HashSet<>(Arrays.asList(
            "access_token", "authorization", "token", "password", "passwd", "secret", "api_key", "apikey"));

    private static final String MASKED_VALUE = "***";

    private static final String NULL_PLACEHOLDER = "null";

    private LogSanitizer() {
    }

    /**
     * 脱敏并截断（使用默认长度上限）
     *
     * @param rawJsonMessage 原始文本，允许为 null
     * @return 可安全写入日志的文本
     */
    public static String sanitize(String rawJsonMessage) {
        return sanitize(rawJsonMessage, DEFAULT_MAX_LENGTH);
    }

    /**
     * 脱敏并截断
     *
     * @param rawJsonMessage 原始文本，允许为 null
     * @param maxLength      输出长度上限，小于 1 时按 1 处理
     * @return 可安全写入日志的文本
     */
    public static String sanitize(String rawJsonMessage, int maxLength) {
        if (rawJsonMessage == null) {
            return NULL_PLACEHOLDER;
        }

        int effectiveMaxLength = Math.max(maxLength, 1);
        String masked = maskSensitiveFields(rawJsonMessage);
        if (masked.length() <= effectiveMaxLength) {
            return masked;
        }
        return masked.substring(0, effectiveMaxLength) + "...(已截断，原始长度 " + rawJsonMessage.length() + ")";
    }

    /**
     * 按字段名递归脱敏
     *
     * <p>只有结构化内容（JSON 对象 / 数组）才存在"字段"可脱敏，因此：
     * <ul>
     *     <li>非结构化内容（纯文本、裸标量）原样返回——避免把纯文本重新序列化成带引号的形式，徒增噪音</li>
     *     <li>不是合法 JSON 时同样原样返回</li>
     * </ul>
     * 两种情况都由调用方的长度截断兜底。
     *
     * @param rawJsonMessage 原始文本
     * @return 脱敏后的文本，或原文
     */
    private static String maskSensitiveFields(String rawJsonMessage) {
        try {
            JsonElement element = GsonUtils.getGson().fromJson(rawJsonMessage, JsonElement.class);
            if (element == null || !(element.isJsonObject() || element.isJsonArray())) {
                return rawJsonMessage;
            }
            maskSensitiveFields(element);
            return element.toString();
        } catch (RuntimeException e) {
            return rawJsonMessage;
        }
    }

    private static void maskSensitiveFields(JsonElement element) {
        if (element == null) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            // 复制一份 entrySet，避免遍历过程中修改对象导致 ConcurrentModificationException
            for (Map.Entry<String, JsonElement> entry : new ArrayList<>(jsonObject.entrySet())) {
                String key = entry.getKey();
                if (key != null && SENSITIVE_FIELD_NAMES.contains(key.toLowerCase(Locale.ROOT))) {
                    jsonObject.addProperty(key, MASKED_VALUE);
                } else {
                    maskSensitiveFields(entry.getValue());
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                maskSensitiveFields(child);
            }
        }
    }
}
