package com.github.theword.queqiao.tool.config.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置路径工具
 *
 * <p>Checker 与 Synchronizer 共用，避免出现两套路径遍历逻辑（Phase 6 §31/§32）。
 *
 * @since 0.6.12
 */
public final class ConfigPaths {

    /**
     * {@code addons} 扩展命名空间的首段名称
     */
    public static final String ADDONS = "addons";

    private ConfigPaths() {
    }

    /**
     * 判断路径是否属于 {@code addons} 扩展命名空间
     *
     * <p><b>按路径段判断，不做字符串前缀匹配</b>：{@code addons2.foo} 不属于 {@code addons}。
     *
     * @param path 点分路径
     * @return true 表示首段是 {@code addons}
     */
    public static boolean isAddonPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        int dot = path.indexOf('.');
        String firstSegment = dot < 0 ? path : path.substring(0, dot);
        return ADDONS.equals(firstSegment);
    }

    /**
     * 取父路径
     *
     * @param path 点分路径
     * @return 父路径；顶层路径返回空串
     */
    static String parentOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(0, dot);
    }

    /**
     * 取最后一段
     *
     * @param path 点分路径
     * @return 最后一段
     */
    static String lastSegmentOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(dot + 1);
    }

    /**
     * 拼接路径
     *
     * @param prefix 前缀（可为空）
     * @param name   名称
     * @return 完整路径
     */
    static String join(String prefix, String name) {
        return prefix.isEmpty() ? name : prefix + "." + name;
    }

    /**
     * 取全部祖先路径（自顶向下，不含自身）
     *
     * @param path 点分路径
     * @return 祖先路径列表
     */
    static List<String> ancestorsOf(String path) {
        List<String> ancestors = new ArrayList<>();
        int index = path.indexOf('.');
        while (index >= 0) {
            ancestors.add(path.substring(0, index));
            index = path.indexOf('.', index + 1);
        }
        return ancestors.isEmpty() ? Collections.<String>emptyList() : ancestors;
    }
}
