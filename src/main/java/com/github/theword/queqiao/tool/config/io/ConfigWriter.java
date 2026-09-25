package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigNode;
import com.github.theword.queqiao.tool.config.ConfigSectionNode;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 配置写出器：<b>ConfigWriteSnapshot → config.yml</b>
 *
 * <p><b>职责边界（Phase 5 最重要的边界）</b>：
 * <pre>
 * ConfigWriter      = 持久化当前状态（save）
 * ConfigSynchronizer= 改造文档使其符合 Schema（sync）
 * </pre>
 * 因此 {@code save} <b>不会</b>删除未知字段（核心未知与 {@code addons} 未知都必须保留），
 * 也不会补齐缺失字段——那属于 Phase 6 的 {@code sync}。
 *
 * <p><b>其他边界</b>：
 * <ul>
 *     <li>只读：不修改 Runtime，也不修改 Document；</li>
 *     <li>deterministic：顺序完全取自 Schema 的声明顺序，绝不按字母排序，
 *         也不依赖任何 HashMap 迭代顺序；</li>
 *     <li>类型安全：标量表示由 {@link com.github.theword.queqiao.tool.config.ConfigCodec#write(Object)}
 *         决定，字符串的 YAML quoting 统一由 {@link #scalar(Object)} 一处负责；</li>
 *     <li>不引入新的 YAML 依赖：读仍由 snakeyaml 负责，写出的文本必须能被它读回。</li>
 * </ul>
 *
 * @since 0.6.12
 */
public final class ConfigWriter {

    /**
     * 缩进单位（两个空格）
     */
    private static final String INDENT_UNIT = "  ";

    /**
     * 渲染成 YAML 文本
     *
     * @param snapshot 写盘快照
     * @return YAML 文本
     */
    public String render(ConfigWriteSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        emitChildren(
                snapshot.getTree().getRoot().getChildren(),
                snapshot.getDocument().getRoot(),
                snapshot,
                0,
                out);
        return out.toString();
    }

    /**
     * 原子写入目标文件（复用既有的 {@link ConfigStore}：临时文件 → fsync → 原子替换 → 5 份备份轮换）
     *
     * <p>渲染先于写盘，因此渲染失败绝不会破坏已有的 {@code config.yml}；
     * 写盘本身也是"先写临时文件再替换"，磁盘满 / 权限不足等 I/O 错误不会造成配置丢失。
     *
     * @param snapshot 写盘快照
     * @param target   目标文件
     * @param logger   日志实现，可为 null
     * @throws IOException 写盘失败
     */
    public void write(ConfigWriteSnapshot snapshot, Path target, Logger logger) throws IOException {
        String yaml = render(snapshot);
        ConfigStore.backupAndWriteAtomically(target, yaml, logger);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    private void emitChildren(
            List<ConfigNode> schemaChildren,
            Map<String, Object> documentMap,
            ConfigWriteSnapshot snapshot,
            int depth,
            StringBuilder out) {
        for (ConfigNode child : schemaChildren) {
            if (child instanceof ConfigSectionNode) {
                emitSection((ConfigSectionNode) child, snapshot, depth, out);
            } else {
                emitKey((ConfigKey<?>) child, snapshot, depth, out);
            }
        }

        // 未知字段：稳定追加到所属层级的末尾（不丢弃，也不参与 Schema）
        Set<String> knownNames = new HashSet<>();
        for (ConfigNode child : schemaChildren) {
            knownNames.add(child.getName());
        }
        for (Map.Entry<String, Object> entry : documentMap.entrySet()) {
            if (knownNames.contains(entry.getKey())) {
                continue;
            }
            emitUnknown(entry.getKey(), entry.getValue(), depth, out);
        }
    }

    private void emitSection(ConfigSectionNode section, ConfigWriteSnapshot snapshot, int depth, StringBuilder out) {
        appendBlankLines(out, section.getBlankLinesBefore());
        appendComments(out, section.getCommentLines(), depth);
        out.append(indent(depth)).append(section.getName()).append(":\n");

        Map<String, Object> documentSection = documentSectionOf(snapshot, section.getPath());
        emitChildren(section.getChildren(), documentSection, snapshot, depth + 1, out);

        appendBlankLines(out, section.getBlankLinesAfter());
    }

    private void emitKey(ConfigKey<?> key, ConfigWriteSnapshot snapshot, int depth, StringBuilder out) {
        appendComments(out, key.getCommentLines(), depth);
        String value = emitValue(key, snapshot, depth);
        out.append(indent(depth)).append(key.getName()).append(':');
        if (value.startsWith("\n")) {
            // 块形式（列表）：冒号后直接换行，避免产生尾随空格
            out.append(value);
        } else {
            out.append(' ').append(value);
        }
        out.append('\n');
    }

    /**
     * 取一个配置项的值并序列化
     *
     * <p>标量表示由 codec 决定（{@code codec.write}），Writer 不再复制一套类型系统。
     * 默认值同样会被写出——生成的 config.yml 本身就是可读文档。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private String emitValue(ConfigKey<?> key, ConfigWriteSnapshot snapshot, int depth) {
        Object value = snapshot.getRuntime().valueOf(key);
        // 通配符捕获无法直接调用 codec.write(T)，这里按 raw 类型调用（类型已由 ConfigKey 保证）
        Object raw = ((ConfigKey) key).getCodec().write(value);
        return emitYamlValue(raw, depth);
    }

    private String emitYamlValue(Object raw, int depth) {
        if (raw instanceof List) {
            List<?> list = (List<?>) raw;
            if (list.isEmpty()) {
                return "[]";
            }
            StringBuilder block = new StringBuilder();
            for (Object item : list) {
                block.append('\n').append(indent(depth + 1)).append("- ").append(scalar(item));
            }
            return block.toString();
        }
        return scalar(raw);
    }

    /**
     * 未知字段（核心未知、{@code addons} 下未加载 Addon 的配置）
     *
     * <p>{@code save} 必须保留它们——"不丢用户数据"优先于"输出与 Schema 完全一致"，
     * 后者是 {@code sync} 的职责。
     */
    @SuppressWarnings("unchecked")
    private void emitUnknown(String name, Object value, int depth, StringBuilder out) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
        String dumped = new Yaml(options).dump(Collections.singletonMap(name, value));
        String prefix = indent(depth);
        for (String line : dumped.split("\\n", -1)) {
            if (!line.isEmpty()) {
                out.append(prefix).append(line).append('\n');
            }
        }
    }

    // ------------------------------------------------------------------
    // 排版与标量
    // ------------------------------------------------------------------

    private void appendComments(StringBuilder out, List<String> commentLines, int depth) {
        for (String line : commentLines) {
            if (line == null || line.isEmpty()) {
                // 不产生孤立的 "#"
                continue;
            }
            out.append(indent(depth)).append("# ").append(line).append('\n');
        }
    }

    /**
     * 空行只在<b>这一处</b>管理，避免不同分支各加一次导致数量不可预测
     */
    private void appendBlankLines(StringBuilder out, int count) {
        for (int i = 0; i < count; i++) {
            out.append('\n');
        }
    }

    private static String indent(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(INDENT_UNIT);
        }
        return sb.toString();
    }

    /**
     * 把一个 YAML-safe 值渲染成标量文本
     *
     * <p><b>quoting 只在这里实现一次</b>：字符串交给 snakeyaml 自己的表示逻辑处理，
     * 因此 {@code "true"} / {@code "123"} / {@code "null"} / {@code "hello: world"}
     * 这类"看起来像其它类型"的字符串会被正确加引号，写回后仍是字符串。
     */
    private static String scalar(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        String text = value.toString();
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return doubleQuote(text);
        }
        return representWithSnakeYaml(text);
    }

    /**
     * 用 snakeyaml 自身的表示逻辑生成标量文本
     *
     * <p>做法是 dump 一个单键映射再取出标量部分——这样 quoting 规则与读取端完全一致，
     * 不会出现"写出的形式读不回来"。若 dump 结果不是单行（例如被折行），退回双引号形式。
     */
    private static String representWithSnakeYaml(String text) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        String dumped = new Yaml(options).dump(Collections.singletonMap("k", text));

        String prefix = "k: ";
        if (!dumped.startsWith(prefix)) {
            return doubleQuote(text);
        }
        String remainder = dumped.substring(prefix.length());
        int newline = remainder.indexOf('\n');
        if (newline < 0) {
            return doubleQuote(text);
        }
        String scalar = remainder.substring(0, newline);
        if (remainder.indexOf('\n', newline + 1) >= 0) {
            // dump 出多行（折行 / 块标量）→ 用确定性的双引号形式
            return doubleQuote(text);
        }
        return scalar;
    }

    private static String doubleQuote(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    /**
     * 取文档中某个区块的原始映射（不存在时返回空映射）
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> documentSectionOf(ConfigWriteSnapshot snapshot, String path) {
        Object value = snapshot.getDocument().get(path);
        return value instanceof Map ? (Map<String, Object>) value : Collections.<String, Object>emptyMap();
    }
}
