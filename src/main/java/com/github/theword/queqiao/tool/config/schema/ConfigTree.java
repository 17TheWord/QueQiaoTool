package com.github.theword.queqiao.tool.config.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置树——某个时刻的<b>不可变 Schema 快照</b>
 *
 * <p>{@link ConfigRegistry} 是可变的注册中心；{@link ConfigTree} 是它的只读快照。
 * 写盘等长时间操作应当基于快照进行，这样不必长时间持有锁（见方案 §21）。
 *
 * @since 0.6.12
 */
public final class ConfigTree {

    private final ConfigSectionNode root;
    private final List<ConfigKey<?>> keys;
    private final Map<String, ConfigKey<?>> keysByPath;

    ConfigTree(ConfigSectionNode root, List<ConfigKey<?>> keys, Map<String, ConfigKey<?>> keysByPath) {
        this.root = root;
        this.keys = Collections.unmodifiableList(keys);
        this.keysByPath = Collections.unmodifiableMap(new LinkedHashMap<>(keysByPath));
    }

    /**
     * @return 根区块（路径为空串）
     */
    public ConfigSectionNode getRoot() {
        return root;
    }

    /**
     * @return 全部配置项，<b>按注册顺序</b>（生成 YAML 时保证顺序稳定）
     */
    public List<ConfigKey<?>> getKeys() {
        return keys;
    }

    /**
     * 按路径查找配置项
     *
     * @param path 点分路径
     * @return 配置项；未注册时返回 null
     */
    public ConfigKey<?> findByPath(String path) {
        return keysByPath.get(path);
    }

    /**
     * @return 是否包含任何配置项
     */
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    /**
     * @return 配置项数量
     */
    public int size() {
        return keys.size();
    }
}
