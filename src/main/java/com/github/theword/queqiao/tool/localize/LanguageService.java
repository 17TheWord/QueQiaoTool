package com.github.theword.queqiao.tool.localize;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语言翻译服务类。
 * <p>
 * 该服务负责从本地文件系统加载 JSON 格式的翻译库，并为事件消息提供多语言转换支持。
 * 采用影子加载机制，支持在不重启插件的情况下通过 reload 方法刷新翻译条目。
 * 核心逻辑包括翻译键的映射、参数格式化以及缺失键的单次预警记录。
 *
 * @since 0.6.0
 */
public class LanguageService {

    /**
     * 内部启用状态。
     * 仅当配置开启且成功载入至少一个有效翻译文件时为 true。
     */
    private boolean internalEnable;

    /**
     * 翻译条目内存缓存。
     * Key 为 Minecraft 标准翻译键，Value 为对应的语言文本模板。
     */
    private final Map<String, String> translations;

    /**
     * 已记录的缺失翻译键集合。
     * 用于确保同一个缺失的 Key 在单次生命周期内只触发一次日志警告。
     */
    private final Set<String> missingKeys;

    /**
     * 是否为模组服务端。
     * 决定了翻译文件存放的基础路径（config 还是 plugins）。
     */
    private final boolean isModServer;

    /**
     * 日志记录器。
     */
    private final Logger logger;

    /**
     * 构造并初始化翻译服务。
     *
     * @param isModServer 是否为模组服务端环境
     * @param logger      外部传入的日志记录器
     */
    public LanguageService(boolean isModServer, Logger logger) {
        this.isModServer = isModServer;
        this.logger = logger;
        this.translations = new HashMap<>();
        this.missingKeys = ConcurrentHashMap.newKeySet();
        this.internalEnable = false;

        reload();
    }

    /**
     * 获取翻译服务当前的内部启用状态。
     *
     * @return true 表示服务已准备就绪且存在载入的翻译条目。
     */
    public boolean isInternalEnable() {
        return internalEnable;
    }

    /**
     * 统一的加载/重载入口。
     * <p>
     * 逻辑步骤：
     * 1. 检查全局配置，若关闭则释放缓存。
     * 2. 扫描指定目录下的所有 JSON 文件。
     * 3. 采用原子级方式更新内存映射，成功更新后重置缺失键记录。
     */
    public synchronized void reload() {
        if (!GlobalContext.getConfig().isEnableTranslation()) {
            this.internalEnable = false;
            this.translations.clear();
            this.missingKeys.clear();
            logger.info("翻译功能已在配置中禁用。");
            return;
        }

        Path configPath = Paths.get("./" + (isModServer ? "config" : "plugins"), BaseConstant.MODULE_NAME, "translate");
        Map<String, String> tempMap = scanAndLoad(configPath.toFile());

        if (!tempMap.isEmpty()) {
            this.translations.clear();
            this.translations.putAll(tempMap);
            this.missingKeys.clear(); // 刷新缺失记录，允许补全后的翻译生效
            this.internalEnable = true;
            logger.info("翻译服务已就绪，成功载入 {} 条翻译条目。", this.translations.size());
        } else {
            if (this.translations.isEmpty()) this.internalEnable = false;
            logger.warn("未加载到有效翻译内容（可能目录为空或格式错误），将维持当前翻译状态。");
        }
    }

    /**
     * 扫描目录并载入翻译数据。
     *
     * @param folder 待扫描的目录对象
     * @return 包含所有解析成功的翻译条目的 Map
     */
    private Map<String, String> scanAndLoad(File folder) {
        Map<String, String> result = new HashMap<>();
        if (folder == null || !folder.exists() || !folder.isDirectory()) return result;

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null) return result;

        for (File file : files) {
            Map<String, String> fileContent = loadTranslateFile(file);
            if (fileContent != null) {
                result.putAll(fileContent);
            }
        }
        return result;
    }

    /**
     * 读取并解析单个 JSON 翻译文件。
     * 文件的 JSON 格式应为 Map[String, String]。
     *
     * @param file 目标 JSON 文件
     * @return 解析后的 Map 对象，若加载失败或格式不符则返回 null
     */
    private Map<String, String> loadTranslateFile(File file) {
        try {
            String jsonContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();

            Map<String, String> data = GsonUtils.getGson().fromJson(jsonContent, type);
            if (data != null && !data.isEmpty()) {
                logger.info("加载文件成功: {} ({} 条)", file.getName(), data.size());
                return data;
            }
        } catch (Exception e) {
            logger.warn("加载翻译文件失败: {}, 原因: {}", file.getName(), e.getMessage());
        }
        return null;
    }

    /**
     * 对外翻译接口。
     * <p>
     * 根据传入的翻译键映射对应的文本，并使用 String.format 填充参数。
     * 若配置禁用、服务未初始化或 Key 不存在，将直接返回原始 Key。
     *
     * @param key  翻译键（例如 death.attack.player）
     * @param args 翻译参数数组，对应模板中的 %s 等占位符
     * @return 翻译并格式化后的文本，或原始 Key
     */
    public String translate(String key, Object[] args) {
        if (!GlobalContext.getConfig().isEnableTranslation()) return key;

        if (!internalEnable) return key;

        String template = translations.get(key);
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
            return String.format(template, args);
        } catch (Exception e) {
            logger.warn("格式化异常，Key: {}, 模板: {}", key, template);
            return template;
        }
    }

    /**
     * 停用并清理翻译服务。
     * 释放内存缓存并重置状态位，通常在插件关闭时调用。
     */
    public void disable() {
        this.internalEnable = false;
        this.translations.clear();
        this.missingKeys.clear();
    }
}