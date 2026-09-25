package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.ConfigFileState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一次配置加载的结果
 *
 * <p><b>为什么未知字段留在这里而不是塞进 Runtime</b>：{@code Config} 只保存
 * <b>已注册 ConfigKey</b> 的值。未知字段属于文档层——只有留在文档层，
 * 未来的 {@code config check} 与 {@code config sync} 才知道"哪些字段是未知的"，
 * 也才能按规则（核心未知可删、addons 未知必须保留）分别处理。
 *
 * @since 0.6.12
 */
public final class ConfigLoadResult {

    private final ConfigFileState state;
    private final ConfigDocument document;
    private final List<String> unknownCorePaths;
    private final List<String> unknownAddonPaths;

    ConfigLoadResult(
            ConfigFileState state,
            ConfigDocument document,
            List<String> unknownCorePaths,
            List<String> unknownAddonPaths) {
        this.state = state;
        this.document = document;
        this.unknownCorePaths = Collections.unmodifiableList(new ArrayList<>(unknownCorePaths));
        this.unknownAddonPaths = Collections.unmodifiableList(new ArrayList<>(unknownAddonPaths));
    }

    /**
     * @return 文件状态（MISSING / EMPTY / VALID）
     */
    public ConfigFileState getState() {
        return state;
    }

    /**
     * @return 完整文档（含未知字段，供后续 check / sync 使用）
     */
    public ConfigDocument getDocument() {
        return document;
    }

    /**
     * @return 当前版本不认识的<b>核心</b>字段路径
     */
    public List<String> getUnknownCorePaths() {
        return unknownCorePaths;
    }

    /**
     * @return {@code addons} 下当前版本不认识的字段路径（<b>必须保留</b>，不与核心未知混为一谈）
     */
    public List<String> getUnknownAddonPaths() {
        return unknownAddonPaths;
    }

    /**
     * @return 是否存在任何未知字段
     */
    public boolean hasUnknown() {
        return !unknownCorePaths.isEmpty() || !unknownAddonPaths.isEmpty();
    }
}
