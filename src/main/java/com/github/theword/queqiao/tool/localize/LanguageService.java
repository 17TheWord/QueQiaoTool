package com.github.theword.queqiao.tool.localize;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 语言翻译服务类。
 * <p>
 * 该服务负责从本地文件系统加载 JSON 格式的翻译库，并为事件消息提供多语言转换支持。
 * 翻译数据通过不可变快照发布，因此翻译调用不需要获取锁。
 *
 * @since 0.6.0
 */
public class LanguageService {

    private final Set<String> missingKeys = ConcurrentHashMap.newKeySet();
    private final boolean isModServer;
    private final Logger logger;

    /**
     * 启用状态和翻译映射一起发布，避免读线程看到彼此不匹配的状态。
     */
    private volatile TranslationState state = TranslationState.disabled();

    /**
     * 构造并初始化翻译服务。
     *
     * @param isModServer 是否为模组服务端环境
     * @param logger      外部传入的日志记录器
     */
    public LanguageService(boolean isModServer, Logger logger) {
        this.isModServer = isModServer;
        this.logger = logger;
        reload();
    }

    /**
     * 获取翻译服务当前的内部启用状态。
     *
     * @return true 表示服务已准备就绪且存在载入的翻译条目。
     */
    public boolean isInternalEnable() {
        return state.enabled;
    }

    /**
     * 重新读取当前翻译目录并发布新状态。
     *
     * <p>所有文件先加载到局部 Map，完成后再整体发布。读取线程只读取 volatile 状态，
     * 不会观察到清空和填充之间的中间状态。并发 reload/disable 由实例锁串行化。
     */
    public synchronized void reload() {
        if (!GlobalContext.getConfig().get(ConfigKeys.ENABLE_TRANSLATION)) {
            publish(TranslationState.disabled());
            logger.info("翻译功能已在配置中禁用。");
            return;
        }

        Path translationDirectory = Paths.get(
                "./" + (isModServer ? "config" : "plugins"), BaseConstant.MODULE_NAME, "translate");
        final Map<String, String> translations;
        try {
            translations = scanAndLoad(translationDirectory);
        } catch (IOException e) {
            // 本轮无法可靠读取目录时保留上一份完整状态，避免短暂 I/O 故障清空翻译。
            logger.warn("读取翻译目录失败，保留当前翻译状态：{}，原因：{}", translationDirectory, e.getMessage());
            return;
        }

        if (translations.isEmpty()) {
            publish(TranslationState.disabled());
            logger.warn("未加载到有效翻译内容，翻译服务已禁用。");
            return;
        }

        publish(new TranslationState(true, translations));
        logger.info("翻译服务已就绪，成功载入 {} 条翻译条目。", translations.size());
    }

    /**
     * 扫描目录并载入翻译数据。
     *
     * @throws IOException 目录存在但无法枚举时抛出；调用方会保留上一份完整状态
     */
    private Map<String, String> scanAndLoad(Path folder) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        if (Files.notExists(folder)) {
            return result;
        }
        if (!Files.isDirectory(folder)) {
            throw new IOException("翻译路径不是目录");
        }

        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(folder)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(files::add);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));

        for (Path file : files) {
            Map<String, String> fileContent = loadTranslateFile(file);
            for (Map.Entry<String, String> entry : fileContent.entrySet()) {
                if (result.containsKey(entry.getKey())) {
                    logger.warn("翻译键重复，后加载文件将覆盖前值：key={}, file={}", entry.getKey(), file.getFileName());
                }
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 读取并解析单个 JSON 翻译文件。文件格式为 JSON object，值必须为字符串。
     * 无效文件或条目会被记录并跳过，不阻断其他文件加载。
     */
    private Map<String, String> loadTranslateFile(Path file) {
        Map<String, String> data = new LinkedHashMap<>();
        try {
            String jsonContent = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            JsonElement root = new JsonParser().parse(jsonContent);
            if (root == null || !root.isJsonObject()) {
                logger.warn("加载翻译文件失败：{}，根节点必须是 JSON object", file.getFileName());
                return data;
            }

            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                JsonElement value = entry.getValue();
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    logger.warn("跳过无效翻译条目：file={}, key={}，翻译值必须是字符串", file.getFileName(), entry.getKey());
                    continue;
                }
                data.put(entry.getKey(), value.getAsString());
            }

            if (!data.isEmpty()) {
                logger.info("加载文件成功: {} ({} 条)", file.getFileName(), data.size());
            }
        } catch (Exception e) {
            logger.warn("加载翻译文件失败: {}, 原因: {}", file.getFileName(), e.getMessage());
        }
        return data;
    }

    /**
     * 对外翻译接口。
     * 根据传入的翻译键映射对应文本，并使用 String.format 填充参数。
     *
     * @param key  翻译键（例如 death.attack.player）
     * @param args 翻译参数数组，对应模板中的 %s 等占位符
     * @return 翻译并格式化后的文本，或原始 Key
     */
    public String translate(String key, Object[] args) {
        TranslationState current = state;
        if (!current.enabled || key == null) {
            return key;
        }

        String template = current.translations.get(key);
        if (template == null || template.isEmpty()) {
            if (missingKeys.add(key)) {
                logger.warn("未找到翻译内容，Key: {} (仅提示一次)", key);
            }
            return key;
        }

        if (args == null || args.length == 0 || !template.contains("%")) {
            return template;
        }

        try {
            // Minecraft 翻译文件使用 %s / %1$s 等位置参数，不能用 Tool.format 的 {} 语义替代。
            return String.format(template, args);
        } catch (Exception e) {
            logger.warn("格式化异常，Key: {}", key);
            return template;
        }
    }

    /**
     * 停用翻译服务。通过发布空状态完成清理，不修改任何已发布 Map。
     */
    public synchronized void disable() {
        publish(TranslationState.disabled());
    }

    private void publish(TranslationState next) {
        state = next;
        // 刷新缺失记录，允许新状态中的补全条目生效并重新提示仍缺失的 key。
        missingKeys.clear();
    }

    /**
     * 不可变翻译快照。Map 在构造时复制后包装，调用方无法修改已发布状态。
     */
    private static final class TranslationState {
        private final boolean enabled;
        private final Map<String, String> translations;

        private TranslationState(boolean enabled, Map<String, String> translations) {
            this.enabled = enabled;
            this.translations = Collections.unmodifiableMap(new LinkedHashMap<>(translations));
        }

        private static TranslationState disabled() {
            return new TranslationState(false, Collections.<String, String>emptyMap());
        }
    }
}
