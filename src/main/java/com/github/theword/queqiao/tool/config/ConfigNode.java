package com.github.theword.queqiao.tool.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 配置树节点
 *
 * <p>节点的公共部分：<b>名称 + 完整路径 + 注释</b>。
 *
 * <p><b>不保存 parent 引用</b>：节点自带完整 {@code path} 就足够了，
 * 遍历时由上层维护路径即可。这样避免了 parent ↔ child 双向引用带来的
 * 生命周期与一致性问题。
 *
 * @since 0.6.12
 */
public abstract class ConfigNode {

    private final String name;
    private final String path;
    private final List<String> commentLines;

    protected ConfigNode(String path, List<String> commentLines) {
        this.path = validatePath(path);
        this.name = lastSegment(this.path);
        this.commentLines = commentLines == null || commentLines.isEmpty()
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(commentLines));
    }

    /**
     * 校验路径格式
     *
     * <p>空路径是<b>根区块</b>的合法路径；配置项不允许空路径（由 {@code ConfigKey.Builder} 拦截）。
     *
     * @param path 点分路径
     * @return 校验通过的路径
     * @throws IllegalArgumentException 路径为 null、全空白或含空段
     */
    private static String validatePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("配置路径不能为 null");
        }
        if (path.isEmpty()) {
            return path;
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("配置路径不能为空白");
        }
        if (trimmed.startsWith(".") || trimmed.endsWith(".") || trimmed.contains("..")) {
            throw new IllegalArgumentException("配置路径不能有空段：" + path);
        }
        return trimmed;
    }

    private static String lastSegment(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }

    /**
     * @return 本节点的名称（路径最后一段，如 {@code port}）
     */
    public String getName() {
        return name;
    }

    /**
     * @return 完整点分路径（如 {@code websocket_server.port}）
     */
    public String getPath() {
        return path;
    }

    /**
     * @return 注释行（可为空列表）
     */
    public List<String> getCommentLines() {
        return commentLines;
    }

    /**
     * 判断本节点路径是否位于给定路径之下
     *
     * @param ancestorPath 候选祖先路径
     * @return true 表示本节点是其后代
     */
    boolean isUnder(String ancestorPath) {
        return path.length() > ancestorPath.length() + 1 && path.startsWith(ancestorPath + ".");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + path + ")";
    }

    /**
     * 便捷构造注释行列表
     *
     * @param lines 注释行
     * @return 列表
     */
    protected static List<String> lines(String... lines) {
        return lines == null ? Collections.<String>emptyList() : Arrays.asList(lines);
    }
}
