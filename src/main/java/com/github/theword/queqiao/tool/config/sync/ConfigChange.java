package com.github.theword.queqiao.tool.config.sync;

/**
 * 一条同步变更
 *
 * @since 0.6.12
 */
public final class ConfigChange {

    /**
     * 变更类型
     *
     * <p>本阶段只有"增"与"删"：类型 / 取值非法<b>不会</b>被自动修改
     * （自动修复可能丢失用户配置意图），因此不需要 {@code MODIFY}。
     */
    public enum Kind {

        /**
         * 补齐缺失字段（使用 Schema 默认值）
         */
        ADD,

        /**
         * 删除核心未知字段
         */
        REMOVE,

        /**
         * 保留（{@code addons} 未知字段、类型/取值非法的字段）
         */
        KEEP
    }

    private final Kind kind;
    private final String path;
    private final Object value;

    ConfigChange(Kind kind, String path, Object value) {
        this.kind = kind;
        this.path = path;
        this.value = value;
    }

    public Kind getKind() {
        return kind;
    }

    public String getPath() {
        return path;
    }

    /**
     * @return {@link Kind#ADD} 时为补齐的默认值；其它情况为文档中的原值或 null
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        switch (kind) {
            case ADD:
                return "+ " + path + " = " + value;
            case REMOVE:
                return "- " + path;
            default:
                return "~ " + path;
        }
    }
}
