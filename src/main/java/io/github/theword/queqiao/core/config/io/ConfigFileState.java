package io.github.theword.queqiao.core.config.io;

/**
 * 配置文件状态
 *
 * <p><b>必须严格区分这四种状态</b>——把 {@link #EMPTY} 与 {@link #INVALID} 混为一谈，
 * 会导致"用户配置文件语法写错 → 被当成空配置 → 用默认值覆盖原文件"这类数据丢失问题。
 *
 * @since 0.6.11
 */
public enum ConfigFileState {

    /**
     * 文件不存在
     *
     * <p>处理：从当前版本的模板生成，然后正常加载。
     */
    MISSING,

    /**
     * 文件存在但没有有效内容（空文件，或只有注释与空白）
     *
     * <p>处理：视为合法的空配置 → 使用默认值。
     * <b>不与 {@link #INVALID} 混淆</b>。
     */
    EMPTY,

    /**
     * 正常 YAML，根节点为 Map
     *
     * <p>处理：进入 Schema 校验与查漏补缺。
     */
    VALID,

    /**
     * YAML 语法错误、结构错误、或根节点不是 Map
     *
     * <p>处理：<b>ERROR 日志 + 原文件保持不动</b>；
     * 绝不把 INVALID 当成 EMPTY，绝不用默认值重新生成并覆盖原文件。
     */
    INVALID
}
