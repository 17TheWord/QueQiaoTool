package io.github.theword.queqiao.core.config.sync;

/**
 * 配置问题严重级别
 *
 * @since 0.6.12
 */
public enum ConfigIssueSeverity {

    /**
     * 错误：用户必须修正（结构 / 类型 / 取值非法）
     */
    ERROR,

    /**
     * 警告：可同步或仅需知晓（缺失字段、未知字段）
     */
    WARNING
}
