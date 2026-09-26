package com.github.theword.queqiao.tool.config.sync;

/**
 * 配置问题类型
 *
 * <p>枚举顺序<b>就是报告的稳定顺序</b>（Phase 6 §12）：先结构、再类型、再取值，
 * 然后是缺失与未知。同类内部按 Schema / Document 的原始顺序，绝不按字母排序。
 *
 * @since 0.6.12
 */
public enum ConfigIssueType {

    /**
     * 结构非法（该是 Mapping 的位置写成了标量等）
     */
    INVALID_STRUCTURE(ConfigIssueSeverity.ERROR),

    /**
     * 类型错误（如 port 写成字符串）
     */
    INVALID_TYPE(ConfigIssueSeverity.ERROR),

    /**
     * 取值不合法（如 port 越界）
     */
    INVALID_VALUE(ConfigIssueSeverity.ERROR),

    /**
     * Schema 有而文档缺失（可用默认值补齐，<b>不是致命错误</b>）
     */
    MISSING(ConfigIssueSeverity.WARNING),

    /**
     * 核心 Schema 不认识的字段（{@code save} 保留、{@code sync} 删除）
     */
    UNKNOWN_CORE(ConfigIssueSeverity.WARNING),

    /**
     * {@code addons} 下当前版本不认识的字段（<b>必须保留</b>）
     */
    UNKNOWN_ADDON(ConfigIssueSeverity.WARNING);

    private final ConfigIssueSeverity severity;

    ConfigIssueType(ConfigIssueSeverity severity) {
        this.severity = severity;
    }

    public ConfigIssueSeverity getSeverity() {
        return severity;
    }
}
