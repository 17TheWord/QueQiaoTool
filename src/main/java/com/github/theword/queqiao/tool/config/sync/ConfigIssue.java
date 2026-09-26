package com.github.theword.queqiao.tool.config.sync;

/**
 * 一条配置问题
 *
 * @since 0.6.12
 */
public final class ConfigIssue {

    private final ConfigIssueType type;
    private final String path;
    private final String message;
    private final Object expected;
    private final Object actual;
    private final Object defaultValue;

    private ConfigIssue(
            ConfigIssueType type, String path, String message, Object expected, Object actual, Object defaultValue) {
        this.type = type;
        this.path = path;
        this.message = message;
        this.expected = expected;
        this.actual = actual;
        this.defaultValue = defaultValue;
    }

    /**
     * 结构非法
     *
     * @param path     路径
     * @param expected 期望的结构
     * @param actual   实际值
     * @return 问题
     */
    static ConfigIssue invalidStructure(String path, String expected, Object actual) {
        return new ConfigIssue(
                ConfigIssueType.INVALID_STRUCTURE,
                path,
                "期望 " + expected + "，实际 " + describe(actual),
                expected,
                actual,
                null);
    }

    /**
     * 类型错误
     *
     * @param path     路径
     * @param expected 期望类型
     * @param actual   实际值
     * @return 问题
     */
    static ConfigIssue invalidType(String path, String expected, Object actual) {
        return new ConfigIssue(
                ConfigIssueType.INVALID_TYPE, path, "期望 " + expected + "，实际 " + describe(actual), expected, actual, null);
    }

    /**
     * 取值非法
     *
     * @param path    路径
     * @param message 具体原因
     * @param actual  实际值
     * @return 问题
     */
    static ConfigIssue invalidValue(String path, String message, Object actual) {
        return new ConfigIssue(ConfigIssueType.INVALID_VALUE, path, message, null, actual, null);
    }

    /**
     * 缺失字段
     *
     * @param path         路径
     * @param defaultValue Schema 声明的默认值
     * @return 问题
     */
    static ConfigIssue missing(String path, Object defaultValue) {
        return new ConfigIssue(ConfigIssueType.MISSING, path, "文档中缺少该配置项", null, null, defaultValue);
    }

    /**
     * 未知核心字段
     *
     * @param path  路径
     * @param value 文档中的值
     * @return 问题
     */
    static ConfigIssue unknownCore(String path, Object value) {
        return new ConfigIssue(
                ConfigIssueType.UNKNOWN_CORE, path, "当前版本不支持该配置项（同步时将删除）", null, value, null);
    }

    /**
     * 未知 addons 字段
     *
     * @param path  路径
     * @param value 文档中的值
     * @return 问题
     */
    static ConfigIssue unknownAddon(String path, Object value) {
        return new ConfigIssue(
                ConfigIssueType.UNKNOWN_ADDON, path, "来自扩展的配置项（同步时会保留）", null, value, null);
    }

    public ConfigIssueType getType() {
        return type;
    }

    public ConfigIssueSeverity getSeverity() {
        return type.getSeverity();
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public Object getExpected() {
        return expected;
    }

    public Object getActual() {
        return actual;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    private static String describe(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName() + "(" + value + ")";
    }

    @Override
    public String toString() {
        return "[" + type + "] " + path + " —— " + message;
    }
}
