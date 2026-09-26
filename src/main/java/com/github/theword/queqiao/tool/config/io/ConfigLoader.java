package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.schema.ConfigKey;
import com.github.theword.queqiao.tool.config.schema.ConfigNode;
import com.github.theword.queqiao.tool.config.schema.ConfigSectionNode;
import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.schema.ConfigTree;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置加载器：<b>Document → Runtime</b>
 *
 * <p><b>边界（本阶段的核心约束）</b>：
 * <ul>
 *     <li><b>不做文件 IO、不 new Yaml</b>——文件读取与 YAML 解析属于 {@code ConfigFileReader}；
 *         Loader 只接收已解析的 {@code Map}，因此测试可以完全绕过文件系统；</li>
 *     <li><b>不是第二个 Config</b>——不持有状态，只负责"把文档可靠地转换成一个完整可提交的
 *         Runtime State"；</li>
 *     <li><b>不吞异常</b>——只处理明确的三类错误（codec 解析错误、validator 错误、文档结构错误），
 *         其余 {@link RuntimeException} 继续暴露，避免把程序缺陷伪装成配置错误。</li>
 * </ul>
 *
 * <pre>
 * ConfigFileReader = 文件 + YAML 解析
 * ConfigLoader     = Document → Config   ← 本类
 * </pre>
 *
 * <p><b>四状态语义</b>（沿用 {@link ConfigFileState}）：
 * <ul>
 *     <li>{@code MISSING} / {@code EMPTY} → 全部使用默认值，不算错误；</li>
 *     <li>{@code VALID} → 进入本类转换；</li>
 *     <li>{@code INVALID} → <b>抛错且不修改 Runtime</b>（EMPTY 与 INVALID 绝不混淆）。</li>
 * </ul>
 *
 * <p><b>原子性</b>：整个 Load 只向 Runtime 提交一次；任何字段的类型/校验失败都会让
 * Runtime 保持 Load 之前的状态。
 *
 * @since 0.6.12
 */
public final class ConfigLoader {

    /**
     * {@code addons} 扩展命名空间
     */
    public static final String ADDONS_KEY = com.github.theword.queqiao.tool.config.sync.ConfigPaths.ADDONS;

    private final ConfigRegistry registry;
    private final Config runtime;

    /**
     * 构造加载器
     *
     * @param registry Schema 注册中心
     * @param runtime  运行时值存储
     */
    public ConfigLoader(ConfigRegistry registry, Config runtime) {
        if (registry == null) {
            throw new IllegalArgumentException("ConfigRegistry 不能为 null");
        }
        if (runtime == null) {
            throw new IllegalArgumentException("Config 不能为 null");
        }
        this.registry = registry;
        this.runtime = runtime;
    }

    /**
     * 加载一份已解析的配置文档
     *
     * @param state       文件状态（由 {@code ConfigFileReader} 判定）
     * @param rawDocument 已解析的根节点，可为 null
     * @return 加载结果（含未知字段信息）
     * @throws ConfigValidationException 文档结构非法、字段类型错误或校验失败
     */
    public ConfigLoadResult load(ConfigFileState state, Map<String, Object> rawDocument) {
        if (state == ConfigFileState.INVALID) {
            // 绝不修改 Runtime，也不把 INVALID 当成 EMPTY
            throw new ConfigValidationException("<config.yml>", "YAML 解析失败；原文件与运行时状态均保持不变");
        }

        if (state == ConfigFileState.MISSING || state == ConfigFileState.EMPTY) {
            runtime.load(ConfigDocument.empty());
            return new ConfigLoadResult(state, ConfigDocument.empty(), Collections.<String>emptyList(), Collections.<String>emptyList());
        }

        // 1. 文档结构校验：根必须是 Mapping，所有 key 必须是 String
        ConfigDocument document = new ConfigDocument(rawDocument);

        // 2. 整个 Load 使用同一份 Schema 快照，避免"前半程 schema A、后半程 schema B"
        ConfigTree schema = registry.snapshot();

        // 3. 区块结构校验：已注册区块与 addons 都必须是 Mapping
        validateSections(schema, document);
        validateAddons(document);

        // 4. 未知字段检测（只记录，不阻止提交）
        List<String> unknownCore = new ArrayList<>();
        List<String> unknownAddon = new ArrayList<>();
        collectUnknown(schema, document, unknownCore, unknownAddon);

        // 5. 一次提交（内部原子：先全部转换校验，再整份替换状态）
        runtime.load(document);

        return new ConfigLoadResult(state, document, unknownCore, unknownAddon);
    }

    /**
     * 校验所有已注册区块在文档中必须是 Mapping
     *
     * <p>与 {@code addons} 的规则保持一致：区块语义上必须是 Mapping，
     * 写成标量或列表属于用户配置错误，必须明确报出而不是静默回退默认值。
     */
    private void validateSections(ConfigTree schema, ConfigDocument document) {
        List<String> sectionPaths = new ArrayList<>();
        collectSectionPaths(schema.getRoot(), sectionPaths);
        for (String sectionPath : sectionPaths) {
            if (sectionPath.isEmpty()) {
                continue;
            }
            if (document.has(sectionPath) && !document.isSection(sectionPath)) {
                throw new ConfigValidationException(
                        sectionPath,
                        "该配置区块必须是 Mapping，实际为 " + describe(document.get(sectionPath)));
            }
        }
    }

    private void collectSectionPaths(ConfigNode node, List<String> paths) {
        if (!(node instanceof ConfigSectionNode)) {
            return;
        }
        if (!node.getPath().isEmpty()) {
            paths.add(node.getPath());
        }
        for (ConfigNode child :
                ((ConfigSectionNode) node).getChildren()) {
            collectSectionPaths(child, paths);
        }
    }

    /**
     * 校验 {@code addons} 本身必须是 Mapping
     */
    private void validateAddons(ConfigDocument document) {
        if (document.has(ADDONS_KEY) && !document.isSection(ADDONS_KEY)) {
            throw new ConfigValidationException(
                    ADDONS_KEY,
                    "addons 必须是配置区块（Mapping），实际为 "
                            + describe(document.get(ADDONS_KEY)));
        }
    }

    /**
     * 收集未知字段：叶子路径集合 减去 已注册路径集合
     *
     * <p>{@code addons} 下的未知字段单独收集——它们属于 Addon，必须保留，
     * 未来 {@code sync} 也只处理核心未知字段。
     */
    private void collectUnknown(
            ConfigTree schema, ConfigDocument document, List<String> unknownCore, List<String> unknownAddon) {
        Set<String> registered = new HashSet<>();
        for (ConfigKey<?> key : schema.getKeys()) {
            registered.add(key.getPath());
        }

        for (String leafPath : document.leafPaths()) {
            if (registered.contains(leafPath)) {
                continue;
            }
            if (ADDONS_KEY.equals(leafPath) || leafPath.startsWith(ADDONS_KEY + ".")) {
                unknownAddon.add(leafPath);
            } else {
                unknownCore.add(leafPath);
            }
        }
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName() + "(" + value + ")";
    }
}
