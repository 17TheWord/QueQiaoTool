package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 配置加载基类
 *
 * <p><b>职责划分</b>（本类只做编排，具体能力各自独立、可复用）：
 * <ul>
 *     <li>{@link ConfigFileReader} —— 读取 + 四态判定（{@code MISSING / EMPTY / VALID / INVALID}）</li>
 *     <li>{@link ConfigSynchronizer} —— 查漏补缺（补默认值、收集未知字段），<b>不写盘</b></li>
 *     <li>{@link ConfigWriter} —— 原子写 + 备份轮换，<b>不在正常启动路径上调用</b></li>
 * </ul>
 *
 * <p><b>正常启动只读不写 {@code config.yml}</b>：避免破坏用户注释、字段顺序与格式，
 * 也避免写盘中断导致配置损坏。用户可通过每次启动刷新的 {@code config.example.yml}
 * 查看当前版本支持的全部配置项、默认值与说明。
 *
 * <p><b>两个文件</b>：
 * <pre>
 * config.example.yml  ← 当前版本官方 Schema / 文档，每次启动从内置资源刷新（可覆盖）
 * config.yml          ← 用户实际配置，正常启动只读
 * </pre>
 *
 * <p><b>错误处理原则</b>：
 * <ul>
 *     <li><b>用户配置错误</b>（类型错误、数值越界）→ 校验阶段显式识别 → 使用默认值 + 明确日志；</li>
 *     <li><b>未预期的 {@link RuntimeException}</b>（程序缺陷）→ ERROR + 堆栈并<b>向上抛出</b>，
 *         <b>绝不</b>静默转换成默认配置——否则真实缺陷会被伪装成"配置问题"而永远查不出来。</li>
 * </ul>
 *
 * @since 0.6.11
 */
public abstract class CommonConfig {

    private static final String RESOURCE_PREFIX = BaseConstant.MOD_ID + "/";

    /**
     * 内置模板资源名——当前版本 Schema 的唯一来源
     */
    private static final String TEMPLATE_RESOURCE_NAME = "config.example.yml";

    /**
     * 释放到配置目录的参考文件名
     */
    private static final String EXAMPLE_FILE_NAME = "config.example.yml";

    private final Logger logger;

    /**
     * 配置根目录覆盖
     *
     * <p>为 {@code null} 时使用默认的相对目录（{@code config} / {@code plugins}，即相对工作目录）；
     * 显式指定时以它为根，使配置读写不依赖工作目录——
     * 生产可用于自定义数据目录，测试可用于 {@code @TempDir} 逐用例隔离。
     */
    private final Path baseDirectory;

    public CommonConfig(Logger logger) {
        this(logger, null);
    }

    /**
     * 构造函数（可指定配置根目录）
     *
     * @param logger        日志实现
     * @param baseDirectory 配置根目录；为 null 时使用默认相对目录
     */
    protected CommonConfig(Logger logger, Path baseDirectory) {
        this.logger = logger;
        this.baseDirectory = baseDirectory;
    }

    public Logger getLogger() {
        return logger;
    }

    // ------------------------------------------------------------------
    // 主流程
    // ------------------------------------------------------------------

    /**
     * 读取并应用配置文件
     *
     * @param configFolder 配置文件所在目录
     * @param fileName     配置文件名
     */
    protected void readConfigFile(String configFolder, String fileName) {
        Path configPath = resolveConfigPath(configFolder, fileName);

        // 1. 每次启动都把内置模板释放为 config.example.yml（当前版本 Schema 参考，允许覆盖）
        refreshExampleFile(configPath);

        // 2. 读取用户配置并判定四态
        ConfigFileReader.Result result = ConfigFileReader.read(configPath);

        if (result.getState() == ConfigFileState.MISSING) {
            logger.info("配置文件 {} 不存在，将从当前版本模板生成。", fileName);
            if (!generateFromTemplate(configPath)) {
                logger.error("生成配置文件 {} 失败，将使用代码内置默认值。", fileName);
                useBuiltInDefaults(fileName);
                return;
            }
            result = ConfigFileReader.read(configPath);
        }

        if (result.getState() == ConfigFileState.INVALID) {
            // 关键：INVALID 绝不覆盖原文件，也绝不当成 EMPTY
            logger.error("配置文件 {} 解析失败：{}", fileName, result.getErrorMessage());
            logger.error("原文件将保持不动（不会被默认值覆盖）。本次使用安全默认配置，请修正后重载。");
            useBuiltInDefaults(fileName);
            return;
        }

        if (result.getState() == ConfigFileState.EMPTY) {
            logger.warn("配置文件 {} 为空，将使用默认配置。可参考 {} 填写。", fileName, EXAMPLE_FILE_NAME);
        }

        // 3. 查漏补缺（只在内存中进行，不写盘）
        Map<String, Object> effective = result.getMap();
        Map<String, Object> template = readTemplateMap();
        if (template == null) {
            logger.warn("内置模板 {} 不可用，跳过配置查漏补缺。", TEMPLATE_RESOURCE_NAME);
        } else {
            logSyncReport(fileName, ConfigSynchronizer.synchronize(template, effective));
        }

        // 4. 应用
        applyConfigValues(effective, fileName);
    }

    /**
     * 读取配置文件内容（不生成、不刷新 example）
     *
     * @param path     路径
     * @param fileName 文件名
     */
    protected void readConfigValues(Path path, String fileName) {
        ConfigFileReader.Result result = ConfigFileReader.read(path);
        if (result.getState() == ConfigFileState.VALID || result.getState() == ConfigFileState.EMPTY) {
            applyConfigValues(result.getMap(), fileName);
        } else {
            logger.warn("配置文件 {} 状态为 {}，将使用内置默认值。", fileName, result.getState());
            useBuiltInDefaults(fileName);
        }
    }

    /**
     * 加载配置内容
     *
     * <p>由具体实现填充自身字段。实现应通过
     * {@link #requireBoolean}/{@link #requireString}/{@link #requireInt}
     * 等安全取值方法读取，使"用户配置写错"表现为明确的配置错误，
     * 而不是 {@link NullPointerException} / {@link ClassCastException}。
     *
     * @param configMap 生效配置（已补全缺失字段）
     */
    protected abstract void loadConfigValues(Map<String, Object> configMap);

    /**
     * 检查配置文件是否存在，不存在则从模板生成
     *
     * <p>保留以便外部子类复用；{@link #readConfigFile} 内部已处理该情形。
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
        generateFromTemplate(path);
    }

    /**
     * 计算并返回配置差异（<b>不写盘</b>）
     *
     * <p>保留该能力供未来的"同步配置"命令或 {@code ConfigMigration} 使用；
     * 正常启动路径不会调用它来覆盖用户文件。
     *
     * @param path     路径
     * @param fileName 文件名
     * @return 差异报告；无法读取时返回空报告
     */
    protected SyncReport synchronizeConfigFile(Path path, String fileName) {
        Map<String, Object> template = readTemplateMap();
        ConfigFileReader.Result result = ConfigFileReader.read(path);
        if (template == null || result.getMap() == null) {
            logger.warn("无法计算配置差异（{}）：模板或用户配置不可读。", fileName);
            return new SyncReport();
        }
        return ConfigSynchronizer.synchronize(template, result.getMap());
    }

    // ------------------------------------------------------------------
    // 安全取值：把"用户配置写错"与"程序缺陷"分开
    // ------------------------------------------------------------------

    /**
     * 读取布尔值
     *
     * @throws ConfigValidationException 字段缺失或类型不符（属用户配置错误）
     */
    protected boolean requireBoolean(Map<String, Object> configMap, String key) {
        return requireBoolean(configMap, key, key);
    }

    /**
     * 读取布尔值（可指定用于报错的完整字段路径）
     *
     * <p>嵌套段场景下 {@code key} 只是段内的键（如 {@code enable}），
     * 而 {@code fieldPath} 是给用户看的完整路径（如 {@code websocket_server.enable}）——
     * 两者必须分开，否则会拿完整路径去段内查找而永远找不到。
     *
     * @param configMap 所在 Map
     * @param key       段内键
     * @param fieldPath 完整字段路径（用于报错与日志）
     * @throws ConfigValidationException 字段缺失或类型不符
     */
    protected boolean requireBoolean(Map<String, Object> configMap, String key, String fieldPath) {
        Object value = configMap.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new ConfigValidationException(fieldPath, "期望 boolean，实际为 " + describe(value));
    }

    /**
     * 读取字符串
     *
     * <p>YAML 中写成 {@code key:}（无值）时解析结果为 {@code null}，
     * 这是常见的"留空"写法，按空串处理——不因此把整份配置判为非法。
     *
     * @throws ConfigValidationException 字段存在但类型不符
     */
    protected String requireString(Map<String, Object> configMap, String key) {
        return requireString(configMap, key, key);
    }

    /**
     * 读取字符串（可指定用于报错的完整字段路径）
     *
     * @throws ConfigValidationException 字段存在但类型不符
     */
    protected String requireString(Map<String, Object> configMap, String key, String fieldPath) {
        Object value = configMap.get(key);
        if (value == null) {
            return "";
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        throw new ConfigValidationException(fieldPath, "期望 string，实际为 " + describe(value));
    }

    /**
     * 读取整数
     *
     * @throws ConfigValidationException 字段缺失或类型不符
     */
    protected int requireInt(Map<String, Object> configMap, String key) {
        return requireInt(configMap, key, key);
    }

    /**
     * 读取整数（可指定用于报错的完整字段路径）
     *
     * @throws ConfigValidationException 字段缺失或类型不符
     */
    protected int requireInt(Map<String, Object> configMap, String key, String fieldPath) {
        Object value = configMap.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new ConfigValidationException(fieldPath, "期望 integer，实际为 " + describe(value));
    }

    /**
     * 读取嵌套段；缺失或非 Map 时返回 null（由调用方按"使用默认值"处理）
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> optionalMap(Map<String, Object> configMap, String key) {
        Object value = configMap.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    /**
     * 读取字符串列表；缺失或类型不符时返回空列表
     */
    @SuppressWarnings("unchecked")
    protected List<String> optionalStringList(Map<String, Object> configMap, String key) {
        Object value = configMap.get(key);
        return value instanceof List ? (List<String>) value : Collections.emptyList();
    }

    private static String describe(Object value) {
        return value == null ? "缺失" : value.getClass().getSimpleName() + "(" + value + ")";
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /**
     * 解析配置文件路径
     *
     * @param configFolder 默认配置目录（{@code config} 或 {@code plugins}）
     * @param fileName     配置文件名
     * @return 形如 {@code <根目录>/queqiao/<文件名>} 的路径
     */
    private Path resolveConfigPath(String configFolder, String fileName) {
        Path root = this.baseDirectory != null ? this.baseDirectory : Paths.get(configFolder);
        return root.resolve(BaseConstant.MODULE_NAME).resolve(fileName);
    }

    /**
     * 把内置模板释放为 {@code config.example.yml}
     *
     * <p>每次启动都覆盖，确保它始终代表<b>当前版本</b>的 Schema。
     * 失败只告警，不影响用户配置的加载。
     */
    private void refreshExampleFile(Path configPath) {
        Path examplePath = configPath.resolveSibling(EXAMPLE_FILE_NAME);
        try (InputStream inputStream = openTemplateStream()) {
            if (inputStream == null) {
                logger.warn("内置模板 {} 不存在，无法刷新参考文件。", TEMPLATE_RESOURCE_NAME);
                return;
            }
            if (examplePath.getParent() != null) {
                Files.createDirectories(examplePath.getParent());
            }
            Files.copy(inputStream, examplePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("刷新参考文件 {} 失败：{}", EXAMPLE_FILE_NAME, e.getMessage());
        }
    }

    /**
     * 从内置模板生成用户配置文件
     *
     * @return 是否生成成功
     */
    private boolean generateFromTemplate(Path configPath) {
        try (InputStream inputStream = openTemplateStream()) {
            if (inputStream == null) {
                logger.warn("默认配置模板 {} 不存在。", TEMPLATE_RESOURCE_NAME);
                return false;
            }
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("已从当前版本模板生成配置文件。");
            return true;
        } catch (IOException e) {
            logger.warn("生成配置文件失败：{}", e.getMessage());
            return false;
        }
    }

    private InputStream openTemplateStream() {
        return CommonConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + TEMPLATE_RESOURCE_NAME);
    }

    private Map<String, Object> readTemplateMap() {
        try (InputStream inputStream = openTemplateStream()) {
            if (inputStream == null) {
                return null;
            }
            ConfigFileReader.Result result = ConfigFileReader.readResource(inputStream);
            return result.getState() == ConfigFileState.VALID ? result.getMap() : null;
        } catch (IOException e) {
            logger.warn("读取内置模板 {} 失败：{}", TEMPLATE_RESOURCE_NAME, e.getMessage());
            return null;
        }
    }

    /**
     * 逐条输出查漏补缺结果
     *
     * <p>不满足于"共 N 项"——必须告诉用户<b>具体哪个字段</b>被补了、被重置了、或者不被认识。
     */
    private void logSyncReport(String fileName, SyncReport report) {
        if (report.isEmpty()) {
            return;
        }
        if (!report.getAdded().isEmpty()) {
            logger.info(
                    "配置 {} 缺失字段已使用默认值（共 {} 项）：{}", fileName, report.getAdded().size(), report.getAdded());
        }
        if (!report.getReset().isEmpty()) {
            logger.warn(
                    "配置 {} 存在类型或范围非法的字段，已重置为默认值（共 {} 项）：{}",
                    fileName,
                    report.getReset().size(),
                    report.getReset());
        }
        if (!report.getUnknown().isEmpty()) {
            logger.warn(
                    "配置 {} 存在当前版本不认识的字段（共 {} 项，已保留但不会生效）：{}。请参考 {} 查看当前版本支持的配置项。",
                    fileName,
                    report.getUnknown().size(),
                    report.getUnknown(),
                    EXAMPLE_FILE_NAME);
        }
        if (!report.getInvalid().isEmpty()) {
            logger.warn(
                    "配置 {} 存在结构不合法的字段（共 {} 项，已原样保留但不会生效）：{}",
                    fileName,
                    report.getInvalid().size(),
                    report.getInvalid());
        }
    }

    /**
     * 应用生效配置
     *
     * <p>只有 {@link ConfigValidationException}（显式识别的用户配置错误）会被捕获并回退默认值；
     * 其它 {@link RuntimeException} 属于程序缺陷，<b>不在此捕获</b>，会带着堆栈向上抛出。
     */
    private void applyConfigValues(Map<String, Object> effective, String fileName) {
        try {
            loadConfigValues(effective);
        } catch (ConfigValidationException e) {
            logger.error("配置文件 {} 存在非法配置项 → {}", fileName, e.getMessage());
            logger.error("已回退到安全默认配置（仅监听回环地址、Rcon 关闭、不鉴权）。请参考 {} 修正后重载。", EXAMPLE_FILE_NAME);
            useBuiltInDefaults(fileName);
        }
    }

    /**
     * 使用代码内置默认值
     *
     * <p><b>不调用 {@link #loadConfigValues}</b>——具体实现的字段初始值本身就是内置默认值，
     * 因此这里天然不会产生"一半新一半旧"的中间状态。
     *
     * <p>注意：具体实现的字段初始值必须与模板默认值保持一致，
     * 该一致性由 {@code ConfigFileSynchronizationTest#fieldDefaultsMatchTemplateDefaults} 守护。
     */
    private void useBuiltInDefaults(String fileName) {
        logger.warn("配置 {} 本次将使用代码内置默认值。", fileName);
    }

    /**
     * 读取并丢弃输入流（供内部工具使用）
     */
    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }
}
