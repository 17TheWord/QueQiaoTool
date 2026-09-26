package io.github.theword.queqiao.core.config;

import io.github.theword.queqiao.core.config.codec.ConfigCodec;
import io.github.theword.queqiao.core.config.schema.ConfigKey;

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
 * <p>快照创建与读取时都会通过 {@link ConfigCodec#copy(Object)} 复制可变值，
 * 因此快照与 Runtime、快照调用方彼此隔离。
 *
 * @since 0.6.12
 */
public final class ConfigSnapshot {

    private final Map<ConfigKey<?>, Object> values;
    private final Set<ConfigKey<?>> userKeys;

    ConfigSnapshot(Map<ConfigKey<?>, Object> values, Set<ConfigKey<?>> userKeys) {
        Map<ConfigKey<?>, Object> copiedValues = new LinkedHashMap<>();
        for (Map.Entry<ConfigKey<?>, Object> entry : values.entrySet()) {
            copiedValues.put(entry.getKey(), copyValue(entry.getKey(), entry.getValue()));
        }
        this.values = Collections.unmodifiableMap(copiedValues);
        this.userKeys = Collections.unmodifiableSet(new LinkedHashSet<>(userKeys));
    }

    /**
     * 取值（未加载过该 Key 时返回其默认值）
     *
     * @param key 配置项
     * @return 值
     */
    public <T> T valueOf(ConfigKey<T> key) {
        if (!values.containsKey(key)) {
            return key.defaultValue();
        }
        return key.getCodec().copy(cast(values.get(key)));
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object copyValue(ConfigKey<?> key, Object value) {
        return ((ConfigKey) key).getCodec().copy(value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}
