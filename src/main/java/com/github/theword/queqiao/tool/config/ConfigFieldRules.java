package com.github.theword.queqiao.tool.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置字段的取值规则
 *
 * <p>集中定义"哪些字段有数值范围要求"。<b>代码是权威</b>；
 * {@code config.example.yml} 的注释里同步写明同样的范围供用户阅读。
 *
 * <p>取值范围只用于校验，<b>不参与</b>查漏补缺的结构处理。
 *
 * @since 0.6.11
 */
final class ConfigFieldRules {

    /**
     * 字段路径 → 闭区间 [最小值, 最大值]
     */
    private static final Map<String, int[]> INT_RANGES = createIntRanges();

    private ConfigFieldRules() {
    }

    private static Map<String, int[]> createIntRanges() {
        Map<String, int[]> ranges = new LinkedHashMap<>();
        // 端口：0 与负数非法，65535 为上限
        ranges.put("websocket_server.port", new int[] {1, 65535});
        ranges.put("rcon.port", new int[] {1, 65535});
        // 重连间隔：0 会导致"立即重连风暴"，因此下限为 1
        ranges.put("websocket_client.reconnect_interval", new int[] {1, 3600});
        // 重连次数：0 表示禁用自动重连（合法）
        ranges.put("websocket_client.reconnect_max_times", new int[] {0, 1000});
        return Collections.unmodifiableMap(ranges);
    }

    /**
     * 判断整数字段的值是否在允许范围内
     *
     * @param fieldPath 字段点分路径
     * @param value     值
     * @return true 表示合法；无范围要求的字段一律返回 true
     */
    static boolean isInRange(String fieldPath, int value) {
        int[] range = INT_RANGES.get(fieldPath);
        return range == null || (value >= range[0] && value <= range[1]);
    }

    /**
     * 获取字段的范围描述，供日志输出
     *
     * @param fieldPath 字段点分路径
     * @return 形如 {@code 1~65535} 的描述；无范围要求时返回 null
     */
    static String describeRange(String fieldPath) {
        int[] range = INT_RANGES.get(fieldPath);
        return range == null ? null : range[0] + "~" + range[1];
    }
}
