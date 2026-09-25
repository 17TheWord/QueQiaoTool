package com.github.theword.queqiao.tool.config.sync;

import com.github.theword.queqiao.tool.config.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigNode;
import com.github.theword.queqiao.tool.config.ConfigSectionNode;
import com.github.theword.queqiao.tool.config.ConfigTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema 索引
 *
 * <p>把 {@link ConfigTree} 展平成"区块路径集合 + 配置项路径 → Key"，
 * 供 Checker 与 Synchronizer <b>共用</b>——避免出现两套 Schema 遍历逻辑（Phase 6 §31）。
 *
 * <p>构造后不可变，可安全并发使用。
 *
 * @since 0.6.12
 */
final class ConfigSchemaIndex {

    private final List<ConfigKey<?>> keys;
    private final Map<String, ConfigKey<?>> keysByPath;
    private final Set<String> sectionPaths;

    ConfigSchemaIndex(ConfigTree tree) {
        this.keys = Collections.unmodifiableList(new ArrayList<>(tree.getKeys()));
        Map<String, ConfigKey<?>> byPath = new LinkedHashMap<>();
        for (ConfigKey<?> key : keys) {
            byPath.put(key.getPath(), key);
        }
        this.keysByPath = Collections.unmodifiableMap(byPath);

        Set<String> sections = new LinkedHashSet<>();
        collectSections(tree.getRoot(), sections);
        this.sectionPaths = Collections.unmodifiableSet(sections);
    }

    private static void collectSections(ConfigNode node, Set<String> sections) {
        if (!(node instanceof ConfigSectionNode)) {
            return;
        }
        if (!node.getPath().isEmpty()) {
            sections.add(node.getPath());
        }
        for (ConfigNode child : ((ConfigSectionNode) node).getChildren()) {
            collectSections(child, sections);
        }
    }

    /**
     * @param path 路径
     * @return 是否为已注册区块
     */
    boolean isSection(String path) {
        return sectionPaths.contains(path);
    }

    /**
     * @param path 路径
     * @return 是否为已注册配置项
     */
    boolean isKey(String path) {
        return keysByPath.containsKey(path);
    }

    /**
     * @return 全部配置项，按<b>声明顺序</b>
     */
    List<ConfigKey<?>> keys() {
        return keys;
    }

    /**
     * @param path 路径
     * @return 配置项；不存在时返回 null
     */
    ConfigKey<?> keyOf(String path) {
        return keysByPath.get(path);
    }
}
