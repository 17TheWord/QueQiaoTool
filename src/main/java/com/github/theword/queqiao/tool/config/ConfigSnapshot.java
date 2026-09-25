package com.github.theword.queqiao.tool.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 运行时状态的<b>不可变快照</b>
 *
 * <p>Writer 只处理快照，不直接读取 {@link Config}——避免"写盘过程中运行时还在变化"
 * 导致写出混合状态。
 *
 * <p><b>注意</b>：快照中的值与运行时共享同一个对象引用（不做深拷贝）。
 * 这是安全的，因为 {@code Config} 从不在原地修改已存储的值——
 * {@code set}/{@code reset} 都是替换整份状态。
 *
 * @since 0.6.12
 */
public final class ConfigSnapshot {

    private final Map<ConfigKey<?>, Object> values;
    private final Set<ConfigKey<?>> userKeys;

    ConfigSnapshot(Map<ConfigKey<?>, Object> values, Set<ConfigKey<?>> userKeys) {
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        this.userKeys = Collections.unmodifiableSet(new LinkedHashSet<>(userKeys));
    }

    /**
     * 取值（未加载过该 Key 时返回其默认值）
     *
     * @param key 配置项
     * @return 值
     */
    public Object valueOf(ConfigKey<?> key) {
        return values.containsKey(key) ? values.get(key) : key.defaultValue();
    }

    /**
     * @param key 配置项
     * @return 值是否来源于用户配置
     */
    public boolean contains(ConfigKey<?> key) {
        return userKeys.contains(key);
    }

    /**
     * @param key 配置项
     * @return 值是否来源于默认值
     */
    public boolean isDefault(ConfigKey<?> key) {
        return !userKeys.contains(key);
    }

    /**
     * @return 快照中包含的全部 Key（按写入顺序）
     */
    public Set<ConfigKey<?>> keys() {
        return values.keySet();
    }
}
