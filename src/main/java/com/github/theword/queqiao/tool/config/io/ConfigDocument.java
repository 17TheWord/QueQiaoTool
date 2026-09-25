package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 已解析的配置文档
 *
 * <p>它是"YAML 解析结果"的唯一承载者，并<b>独占</b>路径解析逻辑——
 * 这样 Loader、Checker、Synchronizer 都不必各自维护第二套路径遍历（避免行为漂移）。
 *
 * <p><b>职责</b>：
 * <ul>
 *     <li>校验根节点必须是 Mapping、且所有 key 必须是 String（否则明确报错，而不是抛 ClassCastException）；</li>
 *     <li>提供<b>完整段匹配</b>的路径查找：{@code websocket_server.port} 不会误匹配 {@code websocket_server.port_backup}；</li>
 *     <li>枚举全部叶子路径，供未知字段检测使用；</li>
 *     <li>保留<b>未知字段</b>——未知字段属于文档层，不进入 {@code Config}。</li>
 * </ul>
 *
 * @since 0.6.12
 */
public final class ConfigDocument {

    /**
     * 根路径（根节点自身）
     */
    public static final String ROOT_PATH = "";

    private final Map<String, Object> root;

    /**
     * 包装一个已解析的配置文档
     *
     * @param root 根节点，必须为 Map 且 key 全为 String
     * @throws ConfigValidationException 根节点不是 Mapping，或存在非 String 的 key
     */
    public ConfigDocument(Map<?, ?> root) {
        this.root = freezeMap(root == null ? Collections.emptyMap() : root, ROOT_PATH);
    }

    /**
     * 空文档
     *
     * @return 空文档
     */
    public static ConfigDocument empty() {
        return new ConfigDocument(Collections.<String, Object>emptyMap());
    }

    /**
     * @return 只读根节点
     */
    public Map<String, Object> getRoot() {
        return root;
    }

    /**
     * 判断路径是否存在
     *
     * <p>用于区分"字段缺失"与"字段存在但值为 null"——两者语义完全不同。
     *
     * @param path 点分路径
     * @return true 表示路径存在
     */
    public boolean has(String path) {
        return locate(path) != ABSENT;
    }

    /**
     * 按点分路径取值（完整段匹配）
     *
     * @param path 点分路径
     * @return 值；路径不存在时返回 null（调用前请先用 {@link #has(String)} 区分）
     */
    public Object get(String path) {
        Object located = locate(path);
        return located == ABSENT ? null : located;
    }

    /**
     * 判断路径存在且是一个 Mapping（区块）
     *
     * @param path 点分路径
     * @return true 表示该路径是区块
     */
    public boolean isSection(String path) {
        return locate(path) instanceof Map;
    }

    /**
     * 枚举全部叶子路径（非 Mapping 的值）
     *
     * <p>供未知字段检测使用：叶子路径集合减去已注册路径集合，即为未知字段。
     *
     * @return 叶子路径列表（按文档顺序）
     */
    public List<String> leafPaths() {
        List<String> paths = new ArrayList<>();
        collectLeafPaths(root, ROOT_PATH, paths);
        return paths;
    }

    @SuppressWarnings("unchecked")
    private static void collectLeafPaths(Map<String, Object> map, String prefix, List<String> paths) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> nested = (Map<String, Object>) value;
                if (nested.isEmpty()) {
                    paths.add(path);
                } else {
                    collectLeafPaths(nested, path, paths);
                }
            } else {
                paths.add(path);
            }
        }
    }

    private static Map<String, Object> freezeMap(Map<?, ?> source, String prefix) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String)) {
                throw new ConfigValidationException(
                        String.valueOf(key),
                        "配置项的键必须是字符串，实际为 "
                                + (key == null ? "null" : key.getClass().getSimpleName()));
            }
            String name = (String) key;
            String path = prefix.isEmpty() ? name : prefix + "." + name;
            copy.put(name, freezeValue(entry.getValue(), path));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object freezeValue(Object value, String path) {
        if (value instanceof Map) {
            return freezeMap((Map<?, ?>) value, path);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(freezeValue(item, path));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    /**
     * 表示"路径不存在"的哨兵
     */
    private static final Object ABSENT = new Object();

    /**
     * 逐级下钻定位（完整段匹配，不做任何模糊匹配）
     */
    @SuppressWarnings("unchecked")
    private Object locate(String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        Map<String, Object> current = root;
        int start = 0;
        while (true) {
            int dot = path.indexOf('.', start);
            if (dot < 0) {
                String leaf = path.substring(start);
                return current.containsKey(leaf) ? current.get(leaf) : ABSENT;
            }
            Object next = current.get(path.substring(start, dot));
            if (!(next instanceof Map)) {
                return ABSENT;
            }
            current = (Map<String, Object>) next;
            start = dot + 1;
        }
    }
}
