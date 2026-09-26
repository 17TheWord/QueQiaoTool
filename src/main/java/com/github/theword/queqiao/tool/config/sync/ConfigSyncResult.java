package com.github.theword.queqiao.tool.config.sync;

import com.github.theword.queqiao.tool.config.io.ConfigDocument;

/**
 * 配置同步结果：计划 + 应用后的新文档
 *
 * <p><b>不写文件</b>：持久化由 {@code ConfigWriter} 负责。
 *
 * @since 0.6.12
 */
public final class ConfigSyncResult {

    private final ConfigSyncPlan plan;
    private final ConfigDocument document;

    ConfigSyncResult(ConfigSyncPlan plan, ConfigDocument document) {
        this.plan = plan;
        this.document = document;
    }

    /**
     * @return 同步计划
     */
    public ConfigSyncPlan getPlan() {
        return plan;
    }

    /**
     * @return 应用计划后的<b>新文档</b>（原文档未被修改）
     */
    public ConfigDocument getDocument() {
        return document;
    }

    /**
     * @return 是否存在实际变更
     */
    public boolean hasChanges() {
        return !plan.isEmpty();
    }
}
