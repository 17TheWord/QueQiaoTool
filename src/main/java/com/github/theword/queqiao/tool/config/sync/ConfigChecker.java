package com.github.theword.queqiao.tool.config.sync;

import com.github.theword.queqiao.tool.config.schema.ConfigKey;
import com.github.theword.queqiao.tool.config.schema.ConfigNode;
import com.github.theword.queqiao.tool.config.schema.ConfigSectionNode;
import com.github.theword.queqiao.tool.config.schema.ConfigTree;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.io.ConfigDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置检查器：<b>只观察，不修改</b>
 *
 * <p><b>输入只有 Schema 与文档，不读 Runtime</b>——检查的是"配置文件与 Schema 是否一致"，
 * 而不是"运行时当前值是什么"。若从 Runtime 判断，缺失字段早已被默认值填掉，
 * 根本无法知道用户文件里到底缺不缺。
 *
 * <p><b>不重复实现类型/取值校验</b>：直接复用 {@link ConfigKey} 的 codec 与 validator
 * （与 Loader 用的是同一套组件），只是把异常转成"问题记录"而不是"加载失败"。
 *
 * @since 0.6.12
 */
public final class ConfigChecker {

    /**
     * 检查文档与 Schema 的差异
     *
     * @param tree     Schema 快照
     * @param document 配置文档，可为 null（等价于空文档）
     * @return 检查结果（问题按类型稳定排序）
     */
    public ConfigCheckResult check(ConfigTree tree, ConfigDocument document) {
        if (tree == null) {
            throw new IllegalArgumentException("ConfigTree 不能为 null");
        }
        ConfigSchemaIndex schema = new ConfigSchemaIndex(tree);
        ConfigDocument doc = document == null ? ConfigDocument.empty() : document;

        List<ConfigIssue> structure = new ArrayList<>();
        List<ConfigIssue> type = new ArrayList<>();
        List<ConfigIssue> value = new ArrayList<>();
        List<ConfigIssue> missing = new ArrayList<>();
        List<ConfigIssue> unknownCore = new ArrayList<>();
        List<ConfigIssue> unknownAddon = new ArrayList<>();

        checkKnown(tree.getRoot(), doc, structure, type, value, missing);
        checkUnknown(doc.getRoot(), "", schema, structure, unknownCore, unknownAddon);

        // 稳定顺序 = 类型枚举顺序（结构 → 类型 → 取值 → 缺失 → 未知核心 → 未知扩展）
        List<ConfigIssue> issues = new ArrayList<>();
        issues.addAll(structure);
        issues.addAll(type);
        issues.addAll(value);
        issues.addAll(missing);
        issues.addAll(unknownCore);
        issues.addAll(unknownAddon);
        return new ConfigCheckResult(issues);
    }

    /**
     * 遍历 Schema：结构、类型、取值、缺失
     */
    private void checkKnown(
            ConfigNode node,
            ConfigDocument document,
            List<ConfigIssue> structure,
            List<ConfigIssue> type,
            List<ConfigIssue> value,
            List<ConfigIssue> missing) {
        if (!(node instanceof ConfigSectionNode)) {
            return;
        }
        for (ConfigNode child : ((ConfigSectionNode) node).getChildren()) {
            String path = child.getPath();
            if (child instanceof ConfigSectionNode) {
                if (document.has(path) && !document.isSection(path)) {
                    structure.add(ConfigIssue.invalidStructure(path, "mapping", document.get(path)));
                    // 结构问题已说明原因，不再逐个上报其子项"缺失"，避免噪声
                    continue;
                }
                checkKnown(child, document, structure, type, value, missing);
            } else {
                checkKey((ConfigKey<?>) child, document, type, value, missing);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void checkKey(
            ConfigKey<?> key,
            ConfigDocument document,
            List<ConfigIssue> type,
            List<ConfigIssue> value,
            List<ConfigIssue> missing) {
        String path = key.getPath();
        if (!document.has(path)) {
            missing.add(ConfigIssue.missing(path, key.defaultValue()));
            return;
        }

        Object raw = document.get(path);
        if (raw == null) {
            type.add(ConfigIssue.invalidType(path, key.getCodec().typeName(), null));
            return;
        }

        Object parsed;
        try {
            parsed = ((ConfigKey) key).getCodec().read(path, raw);
        } catch (ConfigValidationException e) {
            type.add(ConfigIssue.invalidType(path, key.getCodec().typeName(), raw));
            return;
        }

        try {
            ((ConfigKey) key).validate(parsed);
        } catch (ConfigValidationException e) {
            value.add(ConfigIssue.invalidValue(path, e.getMessage(), raw));
        }
    }

    /**
     * 遍历文档：未知字段与结构问题
     */
    @SuppressWarnings("unchecked")
    private void checkUnknown(
            Map<String, Object> documentMap,
            String prefix,
            ConfigSchemaIndex schema,
            List<ConfigIssue> structure,
            List<ConfigIssue> unknownCore,
            List<ConfigIssue> unknownAddon) {
        for (Map.Entry<String, Object> entry : documentMap.entrySet()) {
            String path = ConfigPaths.join(prefix, entry.getKey());
            Object value = entry.getValue();

            if (ConfigPaths.ADDONS.equals(path) && !(value instanceof Map)) {
                // addons 语义上必须是 Mapping，不因为是扩展区就放弃结构校验
                structure.add(ConfigIssue.invalidStructure(path, "mapping", value));
                continue;
            }

            if (schema.isSection(path)) {
                if (value instanceof Map) {
                    checkUnknown((Map<String, Object>) value, path, schema, structure, unknownCore, unknownAddon);
                }
                continue;
            }

            if (schema.isKey(path)) {
                continue;
            }

            if (ConfigPaths.isAddonPath(path)) {
                if (value instanceof Map) {
                    collectAddonLeaves((Map<String, Object>) value, path, unknownAddon);
                } else {
                    unknownAddon.add(ConfigIssue.unknownAddon(path, value));
                }
            } else {
                // 核心未知：整个子树作为一条，避免逐叶子刷屏
                unknownCore.add(ConfigIssue.unknownCore(path, value));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void collectAddonLeaves(Map<String, Object> map, String prefix, List<ConfigIssue> unknownAddon) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = ConfigPaths.join(prefix, entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map) {
                collectAddonLeaves((Map<String, Object>) value, path, unknownAddon);
            } else {
                unknownAddon.add(ConfigIssue.unknownAddon(path, value));
            }
        }
    }
}
