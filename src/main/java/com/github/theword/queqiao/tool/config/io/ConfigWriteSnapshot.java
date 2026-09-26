package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.ConfigSnapshot;
import com.github.theword.queqiao.tool.config.schema.ConfigTree;

/**
 * 写盘快照
 *
 * <p>把 Writer 需要的三样东西<b>一次性冻结</b>：
 * <pre>
 * ConfigTree     ← Schema（顺序、注释、空行）
 * ConfigSnapshot ← 当前值（含 DEFAULT / USER 来源）
 * ConfigDocument ← 原始文档（未知字段留在这里，写盘时必须保留）
 * </pre>
 *
 * <p><b>为什么必须带 Document</b>：如果只按 "Schema + Runtime" 重新生成 YAML，
 * 未知字段（核心未知、{@code addons} 下未加载 Addon 的配置）会被静默丢掉。
 * {@code save} 的语义是"持久化当前状态"，<b>不是</b>"修复配置"——
 * 删除未知字段属于 {@code ConfigSynchronizer} 的职责。
 *
 * @since 0.6.12
 */
public final class ConfigWriteSnapshot {

    private final ConfigTree tree;
    private final ConfigSnapshot runtime;
    private final ConfigDocument document;

    private ConfigWriteSnapshot(ConfigTree tree, ConfigSnapshot runtime, ConfigDocument document) {
        this.tree = tree;
        this.runtime = runtime;
        this.document = document;
    }

    /**
     * 从当前对象构建快照
     *
     * @param registry Schema 注册中心
     * @param runtime  运行时值存储
     * @param document 原始文档（可为 null）
     * @return 写盘快照
     */
    public static ConfigWriteSnapshot of(ConfigRegistry registry, Config runtime, ConfigDocument document) {
        if (registry == null || runtime == null) {
            throw new IllegalArgumentException("registry 与 runtime 不能为 null");
        }
        return new ConfigWriteSnapshot(
                registry.snapshot(), runtime.snapshot(), document == null ? ConfigDocument.empty() : document);
    }

    public ConfigTree getTree() {
        return tree;
    }

    public ConfigSnapshot getRuntime() {
        return runtime;
    }

    public ConfigDocument getDocument() {
        return document;
    }
}
