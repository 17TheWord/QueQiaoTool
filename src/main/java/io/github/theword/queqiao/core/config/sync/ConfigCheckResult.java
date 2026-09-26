package io.github.theword.queqiao.core.config.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 配置检查结果
 *
 * <p>只描述"文档与 Schema 的差异"，<b>不修改任何状态</b>。
 *
 * @since 0.6.12
 */
public final class ConfigCheckResult {

    private final List<ConfigIssue> issues;

    ConfigCheckResult(List<ConfigIssue> issues) {
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    }

    /**
     * @return 全部问题，按 {@link ConfigIssueType} 的枚举顺序稳定排列
     */
    public List<ConfigIssue> getIssues() {
        return issues;
    }

    /**
     * @return 是否存在任何问题
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * @return 是否存在 ERROR 级问题
     */
    public boolean hasErrors() {
        for (ConfigIssue issue : issues) {
            if (issue.getSeverity() == ConfigIssueSeverity.ERROR) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按类型取问题
     *
     * @param type 问题类型
     * @return 该类型的问题列表（保持原顺序）
     */
    public List<ConfigIssue> getIssues(ConfigIssueType type) {
        List<ConfigIssue> result = new ArrayList<>();
        for (ConfigIssue issue : issues) {
            if (issue.getType() == type) {
                result.add(issue);
            }
        }
        return result;
    }

    /**
     * @return 各类型的问题数量
     */
    public Map<ConfigIssueType, Integer> countsByType() {
        Map<ConfigIssueType, Integer> counts = new EnumMap<>(ConfigIssueType.class);
        for (ConfigIssue issue : issues) {
            Integer current = counts.get(issue.getType());
            counts.put(issue.getType(), current == null ? 1 : current + 1);
        }
        return counts;
    }

    @Override
    public String toString() {
        return issues.isEmpty() ? "Configuration is valid." : issues.toString();
    }
}
