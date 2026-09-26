package io.github.theword.queqiao.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LogSanitizer} 单元测试
 *
 * <p>回归目标：debug 日志不得原样输出 token / password 等敏感字段；
 * 无法按字段脱敏时（非法 JSON）必须由长度截断兜底。
 */
class LogSanitizerTest {

    @Test
    @DisplayName("顶层敏感字段被脱敏")
    void masksTopLevelSensitiveFields() {
        String sanitized = LogSanitizer.sanitize("{\"access_token\":\"s3cr3t\",\"api\":\"broadcast\"}");

        assertFalse(sanitized.contains("s3cr3t"), "token 不得出现在日志中：" + sanitized);
        assertTrue(sanitized.contains("***"), "应出现掩码：" + sanitized);
        assertTrue(sanitized.contains("broadcast"), "非敏感字段应保留：" + sanitized);
    }

    @Test
    @DisplayName("嵌套对象与数组中的敏感字段也被脱敏")
    void masksNestedSensitiveFields() {
        String raw = "{\"api\":\"send_rcon_command\",\"data\":{\"command\":\"op Steve\","
                + "\"options\":{\"password\":\"p@ss\"},\"list\":[{\"token\":\"abc123\"}]}}";

        String sanitized = LogSanitizer.sanitize(raw);

        assertFalse(sanitized.contains("p@ss"), "嵌套 password 不得出现：" + sanitized);
        assertFalse(sanitized.contains("abc123"), "数组内 token 不得出现：" + sanitized);
        assertTrue(sanitized.contains("op Steve"), "非敏感内容应保留：" + sanitized);
    }

    @Test
    @DisplayName("字段名匹配忽略大小写")
    void fieldNameMatchingIsCaseInsensitive() {
        String sanitized = LogSanitizer.sanitize("{\"Access_Token\":\"s3cr3t\",\"PASSWORD\":\"p@ss\"}");

        assertFalse(sanitized.contains("s3cr3t"));
        assertFalse(sanitized.contains("p@ss"));
    }

    @Test
    @DisplayName("超长内容被截断并标注原始长度")
    void truncatesOverlongContent() {
        String raw = "{\"api\":\"broadcast\",\"data\":{\"message\":\"" + repeat('x', 5000) + "\"}}";

        String sanitized = LogSanitizer.sanitize(raw);

        assertTrue(sanitized.length() < raw.length(), "应被截断");
        assertTrue(sanitized.contains("已截断"), "应标注截断：" + sanitized);
        assertTrue(sanitized.contains("原始长度 " + raw.length()), "应标注原始长度：" + sanitized);
    }

    @Test
    @DisplayName("非结构化超长内容由长度截断兜底")
    void nonStructuredOverlongInputFallsBackToTruncation() {
        String raw = repeat('y', 3000);

        String sanitized = LogSanitizer.sanitize(raw);

        assertTrue(sanitized.length() < raw.length(), "非结构化内容也应被截断");
        assertTrue(sanitized.contains("已截断"), "应标注截断：" + sanitized);
    }

    @Test
    @DisplayName("短的非结构化文本原样返回（不被重新序列化成带引号形式）")
    void shortNonStructuredInputIsReturnedAsIs() {
        assertEquals("hello", LogSanitizer.sanitize("hello"));
    }

    @Test
    @DisplayName("null 输入安全处理")
    void nullInputIsSafe() {
        assertEquals("null", LogSanitizer.sanitize(null));
    }

    @Test
    @DisplayName("自定义长度上限生效，且非正上限不会抛异常")
    void customMaxLengthIsRespected() {
        assertTrue(LogSanitizer.sanitize("abcdef", 3).startsWith("abc"), "应按上限截断");
        // 非正上限按 1 处理，不抛异常——日志路径上的工具不应抛异常
        assertTrue(LogSanitizer.sanitize("abcdef", 0).startsWith("a"));
        assertTrue(LogSanitizer.sanitize("abcdef", -5).startsWith("a"));
    }

    @Test
    @DisplayName("空对象与 JSON 标量安全处理")
    void emptyObjectAndScalarsAreSafe() {
        assertEquals("{}", LogSanitizer.sanitize("{}"));
        assertEquals("\"plain\"", LogSanitizer.sanitize("\"plain\""));
        assertEquals("null", LogSanitizer.sanitize("null"));
    }

    private static String repeat(char character, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(character);
        }
        return builder.toString();
    }
}
