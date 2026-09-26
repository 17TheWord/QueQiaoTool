package io.github.theword.queqiao.core.config;

/**
 * 配置值来源
 *
 * <p>用于严格区分"<b>用户配置了</b>"与"<b>值恰好等于默认值</b>"：
 * <pre>
 * 用户文件里没有 port           → get=默认值, contains=false, isDefault=true
 * 用户文件里写了 port: 25565    → get=25565,  contains=true,  isDefault=false（即使它等于默认值）
 * </pre>
 *
 * <p>未来可扩展 {@code PROGRAMMATIC} / {@code ENVIRONMENT} / {@code COMMAND} 等来源；
 * 本阶段只实现 {@link #DEFAULT} 与 {@link #USER}。
 *
 * @since 0.6.12
 */
public enum ConfigValueSource {

    /**
     * 值来自 {@code ConfigKey} 声明的默认值（用户文件中不存在该字段）
     */
    DEFAULT,

    /**
     * 值来自用户配置（或运行时显式 {@code set}）
     */
    USER
}
