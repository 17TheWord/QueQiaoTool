package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

/**
 * 配置值校验器
 *
 * <p><b>独立于 {@code ConfigKey} 的核心结构</b>：范围、非空、枚举等约束都实现为校验器，
 * 这样将来出现 {@code URL} / {@code Token} / {@code Duration} / {@code Regex} 等新约束时，
 * <b>不需要修改 {@code ConfigKey} 本身</b>。
 *
 * @param <T> 目标类型
 * @since 0.6.12
 */
public interface ConfigValidator<T> {

    /**
     * 校验配置值
     *
     * @param path  配置路径（用于报错）
     * @param value 已解析的值
     * @throws ConfigValidationException 校验不通过
     */
    void validate(String path, T value);
}
