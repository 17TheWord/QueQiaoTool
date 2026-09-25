package com.github.theword.queqiao.tool.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置查漏补缺的结果
 *
 * <p>只记录<b>字段路径</b>（如 {@code websocket_client.reconnect_interval}），
 * 便于日志逐条输出，而不是只给一个"共 N 项"的数字——用户需要知道<b>具体哪一项</b>被动了。
 *
 * @since 0.6.11
 */
public final class SyncReport {

    /**
     * 使用模板默认值补上的字段（模板有、用户配置缺）
     */
    private final List<String> added = new ArrayList<>();

    /**
     * 因类型或数值范围不合法而被重置为默认值的字段
     */
    private final List<String> reset = new ArrayList<>();

    /**
     * 当前版本不认识的字段（<b>保留在文件中</b>、不参与运行时配置、仅告警）
     */
    private final List<String> unknown = new ArrayList<>();

    /**
     * 存在但结构不合法的字段（如 {@code addons} 不是 Map）
     *
     * <p>同样<b>原样保留</b>、不参与运行时配置；与 {@link #unknown} 分开，
     * 因为"不认识的字段"和"结构写错了的字段"对用户而言是两类不同的问题。
     */
    private final List<String> invalid = new ArrayList<>();

    void addAdded(String fieldPath) {
        added.add(fieldPath);
    }

    void addReset(String fieldPath) {
        reset.add(fieldPath);
    }

    void addUnknown(String fieldPath) {
        unknown.add(fieldPath);
    }

    void addInvalid(String fieldPath) {
        invalid.add(fieldPath);
    }

    /**
     * 是否有任何变更或需要提示的内容
     */
    public boolean isEmpty() {
        return added.isEmpty() && reset.isEmpty() && unknown.isEmpty() && invalid.isEmpty();
    }

    public List<String> getAdded() {
        return Collections.unmodifiableList(added);
    }

    public List<String> getReset() {
        return Collections.unmodifiableList(reset);
    }

    public List<String> getUnknown() {
        return Collections.unmodifiableList(unknown);
    }

    public List<String> getInvalid() {
        return Collections.unmodifiableList(invalid);
    }
}
