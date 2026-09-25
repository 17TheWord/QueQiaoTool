package com.github.theword.queqiao.tool.config.validation;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 常用校验器工厂
 *
 * <p>校验器属于 {@code ConfigKey}，而不是集中在一个庞大的规则表里——
 * 因此将来出现新约束时只需在这里加一个工厂方法，<b>不必改 {@code ConfigKey} 的核心结构</b>。
 *
 * @since 0.6.12
 */
public final class ConfigValidators {

    private ConfigValidators() {
    }

    /**
     * 闭区间范围校验
     *
     * @param min 最小值（含）
     * @param max 最大值（含）
     * @param <T> 可比较的数值类型
     * @return 校验器
     */
    public static <T extends Comparable<T>> ConfigValidator<T> range(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min 不能大于 max：" + min + " > " + max);
        }
        return (path, value) -> {
            if (value == null) {
                throw new ConfigValidationException(path, "值不能为空");
            }
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                throw new ConfigValidationException(
                        path, "值 " + value + " 超出范围 [" + min + ", " + max + "]");
            }
        };
    }

    /**
     * 必须为正数（{@code > 0}）
     *
     * @param <T> 可比较的数值类型
     * @return 校验器
     */
    public static <T extends Comparable<T>> ConfigValidator<T> positive(T zero) {
        return (path, value) -> {
            if (value == null || value.compareTo(zero) <= 0) {
                throw new ConfigValidationException(path, "值必须大于 " + zero + "，实际 " + value);
            }
        };
    }

    /**
     * 字符串非空（允许 null 时不校验，由 codec 决定 null 的语义）
     *
     * @return 校验器
     */
    public static ConfigValidator<String> nonEmpty() {
        return (path, value) -> {
            if (value != null && value.isEmpty()) {
                throw new ConfigValidationException(path, "值不能为空字符串");
            }
        };
    }

    /**
     * 取值必须在给定集合内
     *
     * @param allowed 允许的取值
     * @param <T>     值类型
     * @return 校验器
     */
    @SafeVarargs
    public static <T> ConfigValidator<T> oneOf(T... allowed) {
        List<T> allowedList = Arrays.asList(allowed);
        return (path, value) -> {
            if (!allowedList.contains(value)) {
                throw new ConfigValidationException(path, "值必须是 " + allowedList + " 之一，实际 " + value);
            }
        };
    }

    /**
     * 列表非空（{@code null} 视为空）
     *
     * @param <T> 元素类型
     * @return 校验器
     */
    public static <T> ConfigValidator<Collection<T>> notEmpty() {
        return (path, value) -> {
            if (value == null || value.isEmpty()) {
                throw new ConfigValidationException(path, "列表不能为空");
            }
        };
    }
}
