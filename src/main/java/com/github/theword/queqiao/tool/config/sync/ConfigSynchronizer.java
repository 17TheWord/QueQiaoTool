package com.github.theword.queqiao.tool.config.sync;

import com.github.theword.queqiao.tool.config.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigTree;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.io.ConfigDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置同步器：<b>计划 + 安全的结构修改</b>
 *
 * <p><b>只做三件安全的事</b>（Phase 6 §14）：
 * <pre>
 * 缺失字段        → ADD（用 Schema 默认值补齐）
 * 核心未知字段    → REMOVE（整个子树删除）
 * addons 未知字段 → KEEP（必须保留）
 * </pre>
 *
 * <p><b>明确不做的事</b>：
 * <ul>
 *     <li><b>不自动修复类型/取值错误</b>——{@code port: hello} / {@code port: 99999} 一律保留并报告。
 *         自动"猜"会丢失用户配置意图，那是未来 {@code ConfigMigration} 的职责；</li>
 *     <li><b>不生成注释</b>——注释是 Writer 的事（Synchronizer 只做结构变换）；</li>
 *     <li><b>不执行业务规则</b>——例如 {@code ignored_commands} 的"强制项并集"属于运行时语义
 *         （{@code ConfigKeys.effectiveIgnoredCommands}），不在同步阶段重复一份；</li>
 *     <li><b>不触碰 Runtime，也不写文件</b>——文件同步 ≠ 运行时重载。</li>
 * </ul>
 *
 * <p><b>纯函数风格</b>：{@link #plan} 与 {@link #apply} 都不修改输入；
 * {@code --dry-run} 展示 {@code plan}，真实同步再 {@code apply}——
 * 两者共用同一份计划，diff 必然一致。
 *
 * @since 0.6.12
 */
public final class ConfigSynchronizer {

    /**
     * 计算同步计划（<b>纯计算，不修改任何输入</b>）
     *
     * @param tree     Schema 快照
     * @param document 配置文档，可为 null
     * @return 同步计划
     */
    public ConfigSyncPlan plan(ConfigTree tree, ConfigDocument document) {
        if (tree == null) {
            throw new IllegalArgumentException("ConfigTree 不能为 null");
        }
        ConfigSchemaIndex schema = new ConfigSchemaIndex(tree);
        ConfigDocument doc = document == null ? ConfigDocument.empty() : document;

        List<ConfigChange> additions = new ArrayList<>();
        List<ConfigChange> removals = new ArrayList<>();
        List<ConfigChange> preserved = new ArrayList<>();

        collectKeyChanges(schema, doc, additions, preserved);
        collectStructuralChanges(doc.getRoot(), "", schema, removals, preserved);

        List<ConfigChange> changes = new ArrayList<>();
        changes.addAll(additions);
        changes.addAll(removals);
        changes.addAll(preserved);
        return new ConfigSyncPlan(changes);
    }

    /**
     * 应用计划，产生<b>新文档</b>
     *
     * <p>输入文档不会被修改（内部先做深拷贝）。
     *
     * @param plan     同步计划
     * @param document 原文档
     * @return 新文档
     */
    public ConfigDocument apply(ConfigSyncPlan plan, ConfigDocument document) {
        if (plan == null) {
            throw new IllegalArgumentException("ConfigSyncPlan 不能为 null");
        }
        ConfigDocument source = document == null ? ConfigDocument.empty() : document;
        Map<String, Object> next = deepCopyMap(source.getRoot());

        for (ConfigChange change : plan.getRemovals()) {
            removeAt(next, change.getPath());
        }
        for (ConfigChange change : plan.getAdditions()) {
            putAt(next, change.getPath(), change.getValue());
        }
        return new ConfigDocument(next);
    }

    /**
     * 计算计划并应用
     *
     * @param tree     Schema 快照
     * @param document 配置文档
     * @return 同步结果（计划 + 新文档）
     */
    public ConfigSyncResult synchronize(ConfigTree tree, ConfigDocument document) {
        ConfigSyncPlan plan = plan(tree, document);
        return new ConfigSyncResult(plan, apply(plan, document));
    }

    // ------------------------------------------------------------------
    // 计划
    // ------------------------------------------------------------------

    /**
     * 已知配置项：缺失 → ADD；类型/取值非法 → KEEP（不自动修复）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void collectKeyChanges(
            ConfigSchemaIndex schema, ConfigDocument document, List<ConfigChange> additions, List<ConfigChange> preserved) {
        for (ConfigKey<?> key : schema.keys()) {
            String path = key.getPath();

            if (hasInvalidSectionAncestor(schema, document, path)) {
                // 子字段无法在标量区块下寻址；保留原结构，不能用默认值覆盖父节点。
                continue;
            }

            if (!document.has(path)) {
                // 缺失：使用 Schema 默认值（defaultValue() 已由 codec 复制，不会共享可变对象）
                additions.add(new ConfigChange(ConfigChange.Kind.ADD, path, key.defaultValue()));
                continue;
            }

            Object raw = document.get(path);
            if (raw == null) {
                preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, null));
                continue;
            }

            Object parsed;
            try {
                parsed = ((ConfigKey) key).getCodec().read(path, raw);
            } catch (ConfigValidationException e) {
                preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, raw));
                continue;
            }
            try {
                ((ConfigKey) key).validate(parsed);
            } catch (ConfigValidationException e) {
                preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, raw));
            }
            // 显式写了默认值 → 什么都不做（既不是 ADD 也不是 REMOVE）
        }
    }

    /**
     * 结构层面：核心未知 → REMOVE（整个子树）；{@code addons} 未知 → KEEP（逐叶子）
     */
    @SuppressWarnings("unchecked")
    private void collectStructuralChanges(
            Map<String, Object> documentMap,
            String prefix,
            ConfigSchemaIndex schema,
            List<ConfigChange> removals,
            List<ConfigChange> preserved) {
        for (Map.Entry<String, Object> entry : documentMap.entrySet()) {
            String path = ConfigPaths.join(prefix, entry.getKey());
            Object value = entry.getValue();

            if (schema.isSection(path)) {
                if (value instanceof Map) {
                    collectStructuralChanges((Map<String, Object>) value, path, schema, removals, preserved);
                } else {
                    preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, value));
                }
                // 区块写成标量属于结构非法 → 保留用户内容，不删除
                continue;
            }

            if (schema.isKey(path)) {
                continue;
            }

            if (ConfigPaths.isAddonPath(path)) {
                if (value instanceof Map) {
                    collectAddonKeeps((Map<String, Object>) value, path, preserved);
                } else {
                    preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, value));
                }
            } else {
                // 核心未知：整个子树删除，不留下 foo: {} 空壳
                removals.add(new ConfigChange(ConfigChange.Kind.REMOVE, path, value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectAddonKeeps(Map<String, Object> map, String prefix, List<ConfigChange> preserved) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = ConfigPaths.join(prefix, entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map) {
                collectAddonKeeps((Map<String, Object>) value, path, preserved);
            } else {
                preserved.add(new ConfigChange(ConfigChange.Kind.KEEP, path, value));
            }
        }
    }

    private static boolean hasInvalidSectionAncestor(
            ConfigSchemaIndex schema, ConfigDocument document, String path) {
        for (String ancestor : ConfigPaths.ancestorsOf(path)) {
            if (schema.isSection(ancestor) && document.has(ancestor) && !document.isSection(ancestor)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // 文档变换（全部作用于深拷贝，不动原文档）
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), deepCopy(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Object deepCopy(Object value) {
        if (value instanceof Map) {
            return deepCopyMap((Map<String, Object>) value);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<Object>) value) {
                copy.add(deepCopy(item));
            }
            return copy;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static void removeAt(Map<String, Object> root, String path) {
        Map<String, Object> parent = navigateToParent(root, path);
        if (parent != null) {
            parent.remove(ConfigPaths.lastSegmentOf(path));
        }
    }

    @SuppressWarnings("unchecked")
    private static void putAt(Map<String, Object> root, String path, Object value) {
        Map<String, Object> current = root;
        List<String> ancestors = ConfigPaths.ancestorsOf(path);
        for (String ancestor : ancestors) {
            Object next = current.get(ConfigPaths.lastSegmentOf(ancestor));
            if (!(next instanceof Map)) {
                Map<String, Object> created = new LinkedHashMap<>();
                current.put(ConfigPaths.lastSegmentOf(ancestor), created);
                current = created;
            } else {
                current = (Map<String, Object>) next;
            }
        }
        current.put(ConfigPaths.lastSegmentOf(path), value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> navigateToParent(Map<String, Object> root, String path) {
        Map<String, Object> current = root;
        for (String ancestor : ConfigPaths.ancestorsOf(path)) {
            Object next = current.get(ConfigPaths.lastSegmentOf(ancestor));
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }
        return current;
    }
}
