package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class CommonConfig {

    private static final String RESOURCE_PREFIX = BaseConstant.MOD_ID + "/";
    private static final DumperOptions YAML_DUMP_OPTIONS = createYamlDumpOptions();

    private final Logger logger;

    public CommonConfig(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * 读取配置文件
     *
     * @param configFolder 配置文件所在目录
     * @param fileName     配置文件名
     */
    protected void readConfigFile(String configFolder, String fileName) {
        Path configPath = Paths.get(configFolder, BaseConstant.MODULE_NAME, fileName);
        checkFileExists(configPath, fileName);
        Map<String, Object> configMap = synchronizeAndLoadConfig(configPath, fileName);
        if (configMap != null) {
            loadConfigValuesSafely(configMap, fileName);
        }
    }

    /**
     * 读取配置文件内容
     *
     * @param path     路径
     * @param fileName 文件名
     */
    protected void readConfigValues(Path path, String fileName) {
        Map<String, Object> configMap = readYamlMap(path, fileName, false);
        if (configMap != null) {
            loadConfigValuesSafely(configMap, fileName);
        }
    }

    /**
     * 加载配置文件内容
     *
     * <p>由原版端实现，读取自定义 regex.yml
     *
     * @param configMap 配置文件内容
     */
    protected abstract void loadConfigValues(Map<String, Object> configMap);

    /**
     * 检查配置文件是否存在
     *
     * @param path     路径
     * @param fileName 文件名
     */
    protected void checkFileExists(Path path, String fileName) {
        logger.info("正在寻找配置文件 {}...", fileName);
        if (Files.exists(path)) {
            return;
        }

        logger.warn("配置文件 {} 不存在，将生成默认配置文件。", fileName);
        try (InputStream inputStream = CommonConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + fileName)) {
            if (inputStream == null) {
                logger.warn("默认配置模板 {} 不存在。", fileName);
                return;
            }
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            logger.info("已生成默认配置文件。");
        } catch (IOException e) {
            logger.warn("生成配置文件失败：{}", e.getMessage());
        }
    }

    /**
     * 严格同步字段：
     * <p>1. 当前缺少默认字段 -> 补默认值</p>
     * <p>2. 当前多出未知字段 -> 删除</p>
     * <p>3. 类型不匹配 -> 回退默认值</p>
     */
    protected void synchronizeConfigFile(Path path, String fileName) {
        synchronizeAndLoadConfig(path, fileName);
    }

    private Map<String, Object> synchronizeAndLoadConfig(Path path, String fileName) {
        Map<String, Object> currentMap = readYamlMap(path, fileName, true);
        Map<String, Object> defaultMap = readDefaultYamlMap(fileName);

        if (defaultMap == null) {
            if (currentMap.isEmpty()) {
                logger.warn("配置文件 {} 无法加载，将使用代码中的默认配置。", fileName);
                return null;
            }
            return currentMap;
        }

        SyncStats syncStats = new SyncStats();
        normalizeMap(defaultMap, currentMap, syncStats);
        if (!syncStats.changed()) {
            return currentMap;
        }

        try {
            backupConfig(path);
            writeYamlMap(path, currentMap);
            logger.warn(
                    "配置文件 {} 已同步：新增 {} 项，重置 {} 项，删除 {} 项。",
                    fileName,
                    syncStats.added,
                    syncStats.reset,
                    syncStats.removed
            );
        } catch (IOException e) {
            logger.warn("同步配置文件 {} 失败：{}", fileName, e.getMessage());
        }
        return currentMap;
    }

    private void loadConfigValuesSafely(Map<String, Object> configMap, String fileName) {
        try {
            loadConfigValues(configMap);
        } catch (RuntimeException e) {
            logger.warn("读取配置文件 {} 失败：{}", fileName, e.getMessage());
            logger.warn("将直接使用默认配置项。");
        }
    }

    private Map<String, Object> readDefaultYamlMap(String fileName) {
        try (InputStream inputStream = CommonConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + fileName)) {
            if (inputStream == null) {
                logger.warn("默认配置模板 {} 不存在，跳过同步。", fileName);
                return null;
            }
            return asMap(new Yaml().load(inputStream));
        } catch (RuntimeException | IOException e) {
            logger.warn("默认配置模板 {} 解析失败，跳过同步：{}", fileName, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> readYamlMap(Path path, String fileName, boolean useEmptyMapIfInvalid) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, Object> map = asMap(new Yaml().load(reader));
            if (map != null) {
                return map;
            }
            logger.warn("配置文件 {} 根节点不是 Map。", fileName);
        } catch (RuntimeException | IOException e) {
            logger.warn("解析配置文件 {} 失败：{}", fileName, e.getMessage());
        }
        return useEmptyMapIfInvalid ? new LinkedHashMap<String, Object>() : null;
    }

    private void writeYamlMap(Path path, Map<String, Object> map) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            new Yaml(YAML_DUMP_OPTIONS).dump(map, writer);
        }
    }

    private void backupConfig(Path path) throws IOException {
        Path backupPath = path.resolveSibling(path.getFileName().toString() + ".bak");
        Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object raw) {
        return raw instanceof Map ? (Map<String, Object>) raw : null;
    }

    @SuppressWarnings("unchecked")
    private void normalizeMap(Map<String, Object> defaultMap, Map<String, Object> currentMap, SyncStats syncStats) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!currentMap.containsKey(key)) {
                currentMap.put(key, defaultValue);
                syncStats.added++;
                continue;
            }

            Object currentValue = currentMap.get(key);
            if (defaultValue instanceof Map) {
                if (currentValue instanceof Map) {
                    normalizeMap((Map<String, Object>) defaultValue, (Map<String, Object>) currentValue, syncStats);
                } else {
                    currentMap.put(key, defaultValue);
                    syncStats.reset++;
                }
                continue;
            }

            if (defaultValue instanceof List) {
                if (!(currentValue instanceof List) || !listCompatible((List<?>) defaultValue, (List<?>) currentValue)) {
                    currentMap.put(key, defaultValue);
                    syncStats.reset++;
                }
                continue;
            }

            if (!scalarCompatible(defaultValue, currentValue)) {
                currentMap.put(key, defaultValue);
                syncStats.reset++;
            }
        }

        for (Iterator<String> iterator = currentMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (defaultMap.containsKey(key)) {
                continue;
            }
            iterator.remove();
            syncStats.removed++;
        }
    }

    private boolean listCompatible(List<?> defaultList, List<?> currentList) {
        if (defaultList.isEmpty() || currentList.isEmpty()) {
            return true;
        }

        Object sample = null;
        for (Object value : defaultList) {
            if (value != null) {
                sample = value;
                break;
            }
        }
        if (sample == null) {
            return true;
        }

        for (Object value : currentList) {
            if (value != null && !valueCompatible(sample, value)) {
                return false;
            }
        }
        return true;
    }

    private boolean valueCompatible(Object defaultValue, Object currentValue) {
        if (defaultValue instanceof Map) {
            return currentValue instanceof Map;
        }
        if (defaultValue instanceof List) {
            return currentValue instanceof List;
        }
        return scalarCompatible(defaultValue, currentValue);
    }

    private boolean scalarCompatible(Object defaultValue, Object currentValue) {
        if (defaultValue == null || currentValue == null) {
            return defaultValue == currentValue;
        }
        if (defaultValue instanceof Number && currentValue instanceof Number) {
            return defaultValue.getClass() == currentValue.getClass();
        }
        if (defaultValue instanceof CharSequence && currentValue instanceof CharSequence) {
            return true;
        }
        return defaultValue.getClass().isInstance(currentValue);
    }

    private static DumperOptions createYamlDumpOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(1);
        return options;
    }

    private static final class SyncStats {
        private int added;
        private int reset;
        private int removed;

        private boolean changed() {
            return added != 0 || reset != 0 || removed != 0;
        }
    }
}
