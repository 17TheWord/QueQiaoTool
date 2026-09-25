package com.github.theword.queqiao.tool.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配置文件读取器
 *
 * <p>只负责"读 + 判定状态"，<b>不写盘、不校验、不补默认值</b>。
 * 它是"文件 + YAML 解析"这一层的唯一入口：{@code ConfigLoader} 接收的是它产出的已解析 Map。
 * 严格区分 {@link ConfigFileState} 的四种状态——这是避免"配置写错即被默认值覆盖"的关键。
 *
 * @since 0.6.11
 */
public final class ConfigFileReader {

    private ConfigFileReader() {
    }

    /**
     * 读取结果
     */
    public static final class Result {

        private final ConfigFileState state;
        private final Map<String, Object> map;
        private final String errorMessage;

        private Result(ConfigFileState state, Map<String, Object> map, String errorMessage) {
            this.state = state;
            this.map = map;
            this.errorMessage = errorMessage;
        }

        public ConfigFileState getState() {
            return state;
        }

        /**
         * @return 仅当状态为 {@link ConfigFileState#VALID} / {@link ConfigFileState#EMPTY} 时非 null
         */
        public Map<String, Object> getMap() {
            return map;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 读取磁盘上的配置文件
     *
     * @param path 文件路径
     * @return 读取结果（含四态判定）
     */
    public static Result read(Path path) {
        if (!Files.exists(path)) {
            return new Result(ConfigFileState.MISSING, null, null);
        }

        String content;
        try {
            content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new Result(ConfigFileState.INVALID, null, "读取文件失败：" + e.getMessage());
        }

        return parse(content);
    }

    /**
     * 解析文本内容并判定状态
     *
     * @param content YAML 文本
     * @return 读取结果
     */
    public static Result parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            // 空文件（或只有空白）→ EMPTY，与 INVALID 严格区分
            return new Result(ConfigFileState.EMPTY, new LinkedHashMap<>(), null);
        }

        Object raw;
        try {
            raw = new Yaml().load(content);
        } catch (RuntimeException e) {
            return new Result(ConfigFileState.INVALID, null, "YAML 解析失败：" + e.getMessage());
        }

        if (raw == null) {
            // 只有注释、没有有效内容 → 同样属于 EMPTY，不是解析失败
            return new Result(ConfigFileState.EMPTY, new LinkedHashMap<>(), null);
        }

        if (!(raw instanceof Map)) {
            return new Result(
                    ConfigFileState.INVALID, null, "根节点不是 Map，实际类型为 " + raw.getClass().getSimpleName());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) raw;
        return new Result(ConfigFileState.VALID, map, null);
    }
}
