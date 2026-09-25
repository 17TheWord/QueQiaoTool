package com.github.theword.queqiao.tool.config.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置同步计划
 *
 * <p><b>纯计算结果</b>：只描述"要做什么"，不修改任何输入。
 * {@code --dry-run} 直接展示本对象；真实同步再交给
 * {@link ConfigSynchronizer#apply(ConfigSyncPlan, com.github.theword.queqiao.tool.config.io.ConfigDocument)}
 * 生成新文档——两者共用同一份计划，因此 diff 必然一致。
 *
 * @since 0.6.12
 */
public final class ConfigSyncPlan {

    private final List<ConfigChange> changes;

    ConfigSyncPlan(List<ConfigChange> changes) {
        this.changes = Collections.unmodifiableList(new ArrayList<>(changes));
    }

    /**
     * @return 全部变更（稳定顺序：先补齐、再删除、最后保留）
     */
    public List<ConfigChange> getChanges() {
        return changes;
    }

    /**
     * @return 补齐的字段
     */
    public List<ConfigChange> getAdditions() {
        return byKind(ConfigChange.Kind.ADD);
    }

    /**
     * @return 删除的字段
     */
    public List<ConfigChange> getRemovals() {
        return byKind(ConfigChange.Kind.REMOVE);
    }

    /**
     * @return 保留的字段（{@code addons} 未知字段、类型/取值非法的字段）
     */
    public List<ConfigChange> getPreserved() {
        return byKind(ConfigChange.Kind.KEEP);
    }

    /**
     * @return 是否没有任何变更（此时 apply 后的文档与原文档等价）
     */
    public boolean isEmpty() {
        return getAdditions().isEmpty() && getRemovals().isEmpty();
    }

    private List<ConfigChange> byKind(ConfigChange.Kind kind) {
        List<ConfigChange> result = new ArrayList<>();
        for (ConfigChange change : changes) {
            if (change.getKind() == kind) {
                result.add(change);
            }
        }
        return result;
    }
}
