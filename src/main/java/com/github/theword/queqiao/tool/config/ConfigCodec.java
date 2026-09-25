package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

/**
 * 配置值编解码器
 *
 * <p><b>为什么不用 {@code Class<T>}</b>：{@code Class<?>} 对
 * {@code List<String>} / {@code Map<String, Object>} / {@code Enum} / {@code Duration}
 * 这类类型很快就受限。用 codec 才能把"如何解析"与"如何写出"显式表达出来。
 *
 * <p>实现必须是<b>无状态</b>的：同一个 codec 实例会被所有 {@code ConfigKey} 共享。
 *
 * @param <T> 目标类型
 * @since 0.6.12
 */
public interface ConfigCodec<T> {

    /**
     * 把 YAML 原始值解析为目标类型
     *
     * @param path 配置路径（用于报错）
     * @param raw  原始值，可能为 null（字段存在但值为空）
     * @return 解析结果
     * @throws ConfigValidationException 类型不符或无法解析
     */
    T read(String path, Object raw);

    /**
     * 把目标类型转成可写入 YAML 的原始值
     *
     * @param value 目标值
     * @return YAML 可写出的原始值
     */
    Object write(T value);

    /**
     * 类型名称，用于报错文案（如 {@code integer}）
     *
     * @return 类型名称
     */
    String typeName();

    /**
     * 复制值
     *
     * <p>用于保证"默认值"不会在多个 {@code Config} 实例之间共享同一个可变对象。
     * 不可变类型直接返回原值。
     *
     * @param value 目标值
     * @return 副本
     */
    T copy(T value);
}
