package com.github.theword.queqiao.tool.config;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 配置查漏补缺器
 *
 * <p><b>职责单一</b>：把"当前版本模板有、用户配置缺失"的字段补进内存中的用户配置，
 * 并收集未知字段与非法字段。<b>绝不写盘</b>——正常启动不重写 {@code config.yml}。
 *
 * <p>与 {@link ConfigWriter} 解耦后，本能力可以独立复用：
 * 未来的 {@code ConfigMigration} 或显式的"同步配置"命令可以先用本类算出差异，
 * 再由 {@link ConfigWriter} 落盘。
 *
 * <p><b>未知字段一律保留</b>（不删除）——用户可能在参考其它版本文档、使用第三方扩展、
 * 或提前写好未来配置；自动删除会让用户以为"程序吃掉了自己的配置"。
 *
 * @since 0.6.11
 */
public final class ConfigSynchronizer {

    /**
     * 扩展命名空间
     *
     * <p>Core 只校验它<b>本身是 Map</b>；其内部结构不由 Core Schema 校验、不删除、不告警，
     * 留给未来的 Addon（QueQiao AI / NPC / LLM 等）自行约定。
     */
    public static final String ADDONS_KEY = "addons";

    private ConfigSynchronizer() {
    }

    /**
     * 计算并应用查漏补缺
     *
     * <p><b>会就地修改 {@code userMap}</b>（它是一次全新解析的结果，修改它是安全的），
     * 使其成为"最终生效配置"。
     *
     * @param template 当前版本的模板（来自内置 {@code config.example.yml}）
     * @param userMap  用户配置（会被就地补全）
     * @return 变更与问题报告
     */
    public static SyncReport synchronize(Map<String, Object> template, Map<String, Object> userMap) {
        SyncReport report = new SyncReport();
        mergeTemplateInto(template, userMap, "", report);
        collectProblems(template, userMap, "", report);
        return report;
    }

    /**
     * 把模板中有而用户配置缺失的字段补进用户配置；类型/范围不合法则重置为模板默认值
     */
    private static void mergeTemplateInto(
            Map<String, Object> template, Map<String, Object> userMap, String prefix, SyncReport report) {
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            String fieldPath = path(prefix, key);
            Object defaultValue = entry.getValue();

            if (!userMap.containsKey(key)) {
                userMap.put(key, defaultValue);
                report.addAdded(fieldPath);
                continue;
            }

            Object currentValue = userMap.get(key);

            if (defaultValue instanceof Map) {
                if (currentValue instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedDefault = (Map<String, Object>) defaultValue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedCurrent = (Map<String, Object>) currentValue;
                    mergeTemplateInto(nestedDefault, nestedCurrent, fieldPath, report);
                } else {
                    userMap.put(key, defaultValue);
                    report.addReset(fieldPath);
                }
                continue;
            }

            if (defaultValue instanceof List) {
                if (!(currentValue instanceof List) || !listCompatible((List<?>) defaultValue, (List<?>) currentValue)) {
                    userMap.put(key, defaultValue);
                    report.addReset(fieldPath);
                }
                continue;
            }

            if (!scalarCompatible(defaultValue, currentValue)) {
                userMap.put(key, defaultValue);
                report.addReset(fieldPath);
                continue;
            }

            // 类型正确但数值越界 → 同样重置为默认值
            if (currentValue instanceof Integer && !ConfigFieldRules.isInRange(fieldPath, (Integer) currentValue)) {
                userMap.put(key, defaultValue);
                report.addReset(fieldPath);
            }
        }
    }

    /**
     * 收集未知字段与结构非法的字段（<b>都不删除</b>）
     */
    private static void collectProblems(
            Map<String, Object> template, Map<String, Object> userMap, String prefix, SyncReport report) {
        for (Iterator<Map.Entry<String, Object>> iterator = userMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            String fieldPath = path(prefix, key);
            Object userValue = entry.getValue();

            if (prefix.isEmpty() && ADDONS_KEY.equals(key)) {
                // addons 只校验"本身是 Map"；内部结构完全交给 Addon，既不校验也不告警
                if (!(userValue instanceof Map)) {
                    report.addInvalid(fieldPath);
                }
                continue;
            }

            if (!template.containsKey(key)) {
                report.addUnknown(fieldPath);
                continue;
            }

            Object templateValue = template.get(key);
            if (templateValue instanceof Map && userValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedTemplate = (Map<String, Object>) templateValue;
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedUser = (Map<String, Object>) userValue;
                collectProblems(nestedTemplate, nestedUser, fieldPath, report);
            }
        }
    }

    private static String path(String prefix, String key) {
        return prefix.isEmpty() ? key : prefix + "." + key;
    }

    private static boolean listCompatible(List<?> defaultList, List<?> currentList) {
        if (defaultList.isEmpty() || currentList.isEmpty()) {
            return true;
        }

        Object sample = null;
        for (Object value : defaultList) {
            if (value != null) {
                sample = value;
                break;
            }
        }
        if (sample == null) {
            return true;
        }

        for (Object value : currentList) {
            if (value != null && !valueCompatible(sample, value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valueCompatible(Object defaultValue, Object currentValue) {
        if (defaultValue instanceof Map) {
            return currentValue instanceof Map;
        }
        if (defaultValue instanceof List) {
            return currentValue instanceof List;
        }
        return scalarCompatible(defaultValue, currentValue);
    }

    /**
     * 标量类型兼容判定
     *
     * <p>数字类型<b>要求完全同类</b>：{@code port: 8080} 与 {@code port: 8080.0}
     * 不视为同一配置——后者更可能是用户写错了类型，而不是正常输入。
     */
    private static boolean scalarCompatible(Object defaultValue, Object currentValue) {
        if (defaultValue == null || currentValue == null) {
            return defaultValue == currentValue;
        }
        if (defaultValue instanceof Number && currentValue instanceof Number) {
            return defaultValue.getClass() == currentValue.getClass();
        }
        if (defaultValue instanceof CharSequence && currentValue instanceof CharSequence) {
            return true;
        }
        return defaultValue.getClass().isInstance(currentValue);
    }
}
