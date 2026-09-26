package io.github.theword.queqiao.core.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置区块节点
 *
 * <p>它<b>不是</b>具体配置值，而是配置树中的一个区域（如 {@code websocket_server}）。
 * 携带排版元数据：注释、前后空行数。
 *
 * <p><b>不可变</b>：所有配置方法返回新实例，便于安全共享与并发快照。
 *
 * @since 0.6.12
 */
public final class ConfigSectionNode extends ConfigNode {

    private final List<ConfigNode> children;
    private final int blankLinesBefore;
    private final int blankLinesAfter;

    private ConfigSectionNode(
            String path, List<String> commentLines, List<ConfigNode> children, int blankLinesBefore, int blankLinesAfter) {
        super(path, commentLines);
        this.children = Collections.unmodifiableList(new ArrayList<>(children));
        this.blankLinesBefore = blankLinesBefore;
        this.blankLinesAfter = blankLinesAfter;
    }

    /**
     * 创建一个空区块
     *
     * @param path 完整点分路径
     * @return 区块节点
     */
    public static ConfigSectionNode of(String path) {
        return new ConfigSectionNode(path, Collections.<String>emptyList(), Collections.<ConfigNode>emptyList(), 0, 0);
    }

    /**
     * 设置注释（覆盖）
     *
     * @param commentLines 注释行
     * @return 新实例
     */
    public ConfigSectionNode comment(String... commentLines) {
        return new ConfigSectionNode(getPath(), lines(commentLines), children, blankLinesBefore, blankLinesAfter);
    }

    /**
     * 设置区块前的空行数（只影响生成格式，不影响配置语义）
     *
     * @param count 空行数
     * @return 新实例
     */
    public ConfigSectionNode blankLinesBefore(int count) {
        return new ConfigSectionNode(getPath(), getCommentLines(), children, count, blankLinesAfter);
    }

    /**
     * 设置区块后的空行数（只影响生成格式，不影响配置语义）
     *
     * @param count 空行数
     * @return 新实例
     */
    public ConfigSectionNode blankLinesAfter(int count) {
        return new ConfigSectionNode(getPath(), getCommentLines(), children, blankLinesBefore, count);
    }

    /**
     * 追加子节点
     *
     * @param child 子节点
     * @return 新实例
     */
    ConfigSectionNode withChild(ConfigNode child) {
        List<ConfigNode> next = new ArrayList<>(children);
        next.add(child);
        return new ConfigSectionNode(getPath(), getCommentLines(), next, blankLinesBefore, blankLinesAfter);
    }

    /**
     * @return 子节点（有序，只读）
     */
    public List<ConfigNode> getChildren() {
        return children;
    }

    public int getBlankLinesBefore() {
        return blankLinesBefore;
    }

    public int getBlankLinesAfter() {
        return blankLinesAfter;
    }
}
