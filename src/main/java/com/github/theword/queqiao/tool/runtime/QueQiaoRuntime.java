package com.github.theword.queqiao.tool.runtime;

import com.github.theword.queqiao.tool.config.io.ConfigFileReader;
import com.github.theword.queqiao.tool.config.io.ConfigFileState;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.ConfigSnapshot;
import com.github.theword.queqiao.tool.config.io.ConfigDocument;
import com.github.theword.queqiao.tool.config.io.ConfigLoadResult;
import com.github.theword.queqiao.tool.config.io.ConfigLoader;
import com.github.theword.queqiao.tool.config.io.ConfigStore;
import com.github.theword.queqiao.tool.config.io.ConfigWriteSnapshot;
import com.github.theword.queqiao.tool.config.io.ConfigWriter;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.localize.LanguageService;
import com.github.theword.queqiao.tool.protocol.handler.status.ServerStatusCollector;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.github.theword.queqiao.tool.utils.Tool;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class QueQiaoRuntime {

    /**
     * 空对象使用的日志实现：不输出任何内容，但非 null
     */
    private static final Logger NOP_LOGGER = NOPLogger.NOP_LOGGER;

    // ------------------------------------------------------------------
    // 构造后不再变化的字段：用 final 保证安全发布
    // ------------------------------------------------------------------

    private final Gson gson;
    private final HandleApiService handleApiService;
    private final HandleCommandReturnMessageService handleCommandReturnMessageService;
    private final String serverVersion;
    private final String serverType;
    private final boolean modServer;

    /**
     * 协议分发入口
     *
     * <p>属于平台级能力而非 WebSocket 传输层细节：分发逻辑与传输方式无关，
     * 因此在此处创建唯一实例，再注入给各传输层（当前为 WebSocket，未来可含 HTTP）。
     *
     * <p>该对象构造后即不可变，可安全地在多个连接/线程间共享。
     */
    private final HandleProtocolMessage handleProtocolMessage;

    // ------------------------------------------------------------------
    // 会随 start / reload / shutdown 变化的字段
    //
    // 标记为 volatile：这些字段由 init / reload 线程写入，由游戏线程与 WebSocket 线程读取。
    // 单个字段的可见性由此得到保证，但<b>跨字段的原子快照没有保证</b>——
    // 并发 reload 期间可能瞬时读到新旧混合的值。如需绝对一致，
    // 应改为"不可变状态对象整体替换"。当前 reload 本身会输出日志与消息，
    // 瞬时不一致可接受，故不为此引入额外复杂度。
    // ------------------------------------------------------------------

    /**
     * 配置 Schema（整个 Runtime 只有一份）
     */
    private final ConfigRegistry configRegistry;

    /**
     * 配置运行时状态（<b>唯一</b>配置状态来源）
     */
    private volatile Config config;

    /**
     * 当前配置文档（保留未知字段与原始结构，供 Writer / Checker / Synchronizer 使用）
     *
     * <p>不能只凭 Runtime 重建文档——Runtime 不知道未知字段与文档结构。
     */
    private volatile ConfigDocument configDocument;

    /**
     * 日志实现
     *
     * <p>非 final：为兼容既有的 {@link #setLogger(Logger)} 公开接口。
     *
     * <p>注意：{@link HandleProtocolMessage} 等协作者在构造时即捕获 logger，
     * 因此运行期替换 logger 只会影响之后才去读 {@code GlobalContext.getLogger()} 的路径
     * （如 {@code Tool.debugLog}），不会改变已构造对象的日志输出。
     */
    private volatile Logger logger;

    private volatile WebsocketManager websocketManager;

    /** 串行化 Runtime 对 RCON 客户端的创建、替换和关闭。 */
    private final Object rconLifecycleLock = new Object();

    private volatile RconClient rconClient;

    private volatile JsonElement messagePrefixJsonElement;

    private volatile LanguageService languageService;

    private QueQiaoRuntime(
            boolean modServer,
            String serverVersion,
            String serverType,
            HandleApiService handleApiService,
            HandleCommandReturnMessageService handleCommandReturnMessageService,
            Logger logger) {
        this.modServer = modServer;
        this.serverVersion = serverVersion;
        this.serverType = serverType;
        this.handleApiService = handleApiService;
        this.handleCommandReturnMessageService = handleCommandReturnMessageService;
        this.logger = logger;
        this.gson = GsonUtils.getGson();
        // 配置系统接线：Schema 只注册一次，Config 只建一份，全局唯一配置状态
        this.configRegistry = new ConfigRegistry();
        ConfigKeys.registerAll(this.configRegistry);
        this.config = new Config(this.configRegistry);
        this.configDocument = ConfigDocument.empty();
        // 平台 API 实现与 RCON 执行器由协议层注入，协议层因此不再读 GlobalContext。
        // 这里传入 this::sendRconCommand 是安全的：该 lambda 只在收到请求时才会被调用，
        // 此时对象早已构造完成（构造期间不会被发布）。
        this.handleProtocolMessage = new HandleProtocolMessage(logger, this.gson, handleApiService, this::sendRconCommand);
    }

    /**
     * 构造"最小可用运行时"（空对象）
     *
     * <p>用于尚未 {@code init} 或已 {@code shutdown} 的状态。
     *
     * <p>此前该状态返回一个<b>所有字段均为 null</b> 的实例，导致
     * {@code GlobalContext.shutdown()} / {@code sendEvent()} 等方法直接抛
     * {@link NullPointerException}——空对象名不副实。
     *
     * <p>现在保证：{@link #getLogger()} 返回不输出内容的 NOP 日志；
     * {@link #getConfig()} 返回<b>不触碰文件系统</b>的默认配置；{@link #getGson()} 可用。
     * WebSocket / Rcon / 语言服务仍为 null，相关入口已做空值防护。
     *
     * @return 最小可用运行时
     */
    public static QueQiaoRuntime empty() {
        return new QueQiaoRuntime(
                false,
                null,
                null,
                null,
                null,
                NOP_LOGGER
        );
    }

    public static QueQiaoRuntime create(boolean modServer, String serverVersion, String serverType, HandleApiService handleApiService, HandleCommandReturnMessageService handleCommandReturnMessageService) {
        return create(modServer, serverVersion, serverType, handleApiService, handleCommandReturnMessageService, null);
    }

    /**
     * 创建运行时并在配置加载前注册扩展配置。
     *
     * @param configurer 可选的启动期 Schema 注册回调
     * @return 尚未启动的运行时
     */
    public static QueQiaoRuntime create(
            boolean modServer,
            String serverVersion,
            String serverType,
            HandleApiService handleApiService,
            HandleCommandReturnMessageService handleCommandReturnMessageService,
            Consumer<ConfigRegistry> configurer) {
        Logger runtimeLogger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        QueQiaoRuntime runtime = new QueQiaoRuntime(
                modServer,
                serverVersion,
                serverType,
                handleApiService,
                handleCommandReturnMessageService,
                runtimeLogger
        );
        if (configurer != null) {
            configurer.accept(runtime.configRegistry);
        }
        return runtime;
    }

    /**
     * 加载配置文件并提交到 {@link Config}
     *
     * <p><b>四状态语义</b>（沿用 {@code ConfigFileState}）：
     * <ul>
     *     <li>{@code MISSING} / {@code EMPTY}：全部使用 Schema 默认值，并生成一份完整的
     *         带注释 {@code config.yml}（首次启动体验）；</li>
     *     <li>{@code VALID}：加载后<b>只读不写</b>——正常启动不得无条件重写用户文件；</li>
     *     <li>{@code INVALID}：抛出异常且<b>不修改运行时状态</b>，由上层终止初始化。</li>
     * </ul>
     */
    private void loadConfig() {
        Path configPath = ConfigStore.resolveConfigPath(modServer);
        ConfigFileReader.Result result = ConfigFileReader.read(configPath);
        ConfigLoader loader = new ConfigLoader(configRegistry, config);

        if (result.getState() == ConfigFileState.INVALID) {
            // 交给 Loader 抛出明确异常；运行时状态保持不变
            loader.load(ConfigFileState.INVALID, null);
        }

        if (result.getState() == ConfigFileState.MISSING || result.getState() == ConfigFileState.EMPTY) {
            logger.warn("配置文件 {} 不存在或为空，将使用默认配置并生成完整配置文件。", configPath);
            ConfigLoadResult loaded = loader.load(result.getState(), result.getMap());
            configDocument = loaded.getDocument();
            writeConfigFile(configPath);
            return;
        }

        ConfigLoadResult loaded = loader.load(result.getState(), result.getMap());
        configDocument = loaded.getDocument();
        logUnknownFields(loaded);
    }

    /**
     * 用当前 Schema + 运行时值 + 文档生成 config.yml（唯一 YAML 输出入口）
     */
    private void writeConfigFile(Path configPath) {
        try {
            new ConfigWriter().write(
                    ConfigWriteSnapshot.of(configRegistry, config, configDocument), configPath, logger);
        } catch (IOException e) {
            logger.warn("生成配置文件 {} 失败：{}", configPath, e.getMessage());
        }
    }

    private void logUnknownFields(ConfigLoadResult loaded) {
        if (!loaded.getUnknownCorePaths().isEmpty()) {
            logger.warn("配置文件存在当前版本不支持的字段（已保留、不会生效）：{}", loaded.getUnknownCorePaths());
        }
        if (!loaded.getUnknownAddonPaths().isEmpty()) {
            logger.info("检测到 {} 个扩展配置项（已保留）：{}",
                    loaded.getUnknownAddonPaths().size(), loaded.getUnknownAddonPaths());
        }
    }

    /**
     * @return 当前配置文档（含未知字段）
     */
    public ConfigDocument getConfigDocument() {
        return configDocument;
    }

    /**
     * @return 配置 Schema
     */
    public ConfigRegistry getConfigRegistry() {
        return configRegistry;
    }

    public void start() {
        logger.info(BaseConstant.LAUNCHING);

        // 核心与 Addon 在启动期完成注册；加载配置前冻结 Schema。
        configRegistry.freeze();

        // §6 固定顺序：先把配置加载并提交，再启动任何依赖配置的服务。
        // 配置非法时这里会抛异常，从而不会出现"半套配置 + 服务已启动"的状态（§12/§38）。
        loadConfig();

        logger.info(BaseConstant.INITIALIZED);

        messagePrefixJsonElement = initMessagePrefixJsonObject(config.get(ConfigKeys.MESSAGE_PREFIX));
        languageService = new LanguageService(modServer, logger);
        ServerStatusCollector.initPingTarget(logger);
        ServerStatusCollector.startRefreshScheduler(
                config.get(ConfigKeys.Status.REFRESH_INTERVAL_SECONDS), logger);
        initWebsocketManager();
        initRconClient();
    }

    public void reload(Object commandReturner) {
        loadConfig();
        messagePrefixJsonElement = initMessagePrefixJsonObject(config.get(ConfigKeys.MESSAGE_PREFIX));
        LanguageService service = languageService;
        if (service != null) {
            service.reload();
        }
        ServerStatusCollector.initPingTarget(logger);
        ServerStatusCollector.updateRefreshInterval(config.get(ConfigKeys.Status.REFRESH_INTERVAL_SECONDS));
        WebsocketManager manager = websocketManager;
        if (manager != null) {
            manager.restart(config, commandReturner);
        }
        restartRconClient();
        if (handleCommandReturnMessageService != null) {
            handleCommandReturnMessageService.sendReturnMessage(commandReturner, CommandConstant.RELOAD_CONFIG);
        }
    }

    /**
     * 关闭运行时
     *
     * <p><b>幂等</b>：重复调用不会抛异常。各协作者在关闭后被置空，
     * 因此"尚未启动就关闭"与"关闭两次"都是安全的。
     */
    public void shutdown() {
        ServerStatusCollector.stopRefreshScheduler();

        WebsocketManager manager = websocketManager;
        if (manager != null) {
            manager.stop(1000, WebsocketConstantMessage.SHUTDOWN, null);
            websocketManager = null;
        }

        synchronized (rconLifecycleLock) {
            RconClient client = rconClient;
            rconClient = null;
            if (client != null) {
                client.stop();
            }
        }

        LanguageService service = languageService;
        if (service != null) {
            service.disable();
        }

        logger.info("鹊桥已关闭");
    }

    /**
     * 分发事件
     *
     * <p>运行时空对象状态下静默丢弃，不抛异常。
     *
     * @param baseEvent 事件
     */
    public void sendEvent(BaseEvent baseEvent) {
        WebsocketManager manager = websocketManager;
        if (manager == null) {
            Tool.debugLog("运行时尚未启动或已关闭，事件未分发");
            return;
        }
        // 事件对象的构造不再读全局状态；服务器上下文在发布前统一填充
        baseEvent.fillServerContext(config.get(ConfigKeys.SERVER_NAME), serverVersion, serverType);
        manager.sendEvent(baseEvent);
    }

    private void initWebsocketManager() {
        websocketManager = new WebsocketManager(logger, gson, handleCommandReturnMessageService, handleProtocolMessage, config);
        websocketManager.start(null);
    }

    private void initRconClient() {
        synchronized (rconLifecycleLock) {
            ConfigSnapshot configSnapshot = config.snapshot();
            if (configSnapshot.valueOf(ConfigKeys.Rcon.ENABLE)) {
                RconClient client = createRconClient(configSnapshot);
                client.connect();
                rconClient = client;
            } else {
                rconClient = null;
                logger.info("Rcon 未启用，跳过 Rcon 客户端初始化");
            }
        }
    }

    private void restartRconClient() {
        synchronized (rconLifecycleLock) {
            RconClient previous = rconClient;
            ConfigSnapshot configSnapshot = config.snapshot();
            if (!configSnapshot.valueOf(ConfigKeys.Rcon.ENABLE)) {
                rconClient = null;
                if (previous != null) {
                    previous.stop();
                    logger.info("Rcon 已根据新配置禁用并关闭连接");
                }
                return;
            }

            RconClient replacement = createRconClient(configSnapshot);
            replacement.connect();
            // 先发布新客户端，再关闭旧客户端；已经拿到旧引用的调用由旧客户端自身串行化。
            rconClient = replacement;
            if (previous != null) {
                previous.stop();
            }
        }
    }

    private RconClient createRconClient(ConfigSnapshot source) {
        return new RconClient(
                logger,
                source.valueOf(ConfigKeys.Rcon.PORT),
                source.valueOf(ConfigKeys.Rcon.PASSWORD));
    }

    public String sendRconCommand(String command) throws RconException {
        if (!config.get(ConfigKeys.Rcon.ENABLE)) {
            throw RconException.disabled();
        }
        RconClient client = rconClient;
        if (client == null) {
            throw RconException.disconnected();
        }
        return client.sendCommand(command);
    }

    public JsonElement initMessagePrefixJsonObject(String messagePrefixText) {
        if (messagePrefixText == null) {
            messagePrefixText = "[鹊桥]";
        }

        String criteria = messagePrefixText.trim();

        if (criteria.isEmpty()) {
            JsonObject emptyObj = new JsonObject();
            emptyObj.addProperty("text", "");
            logger.info("消息前缀配置为空，已禁用前缀显示。");
            return emptyObj;
        }

        if ((criteria.startsWith("{") && criteria.endsWith("}")) || (criteria.startsWith("[") && criteria.endsWith("]"))) {
            try {
                JsonElement element = gson.fromJson(messagePrefixText, JsonElement.class);

                if (element.isJsonObject()) {
                    logger.info("消息前缀已成功解析为 MC 组件格式 (JSON Object)。");
                    return element;
                } else if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    if (!(array.size() == 0) && array.get(0).isJsonObject()) {
                        logger.info("消息前缀已成功解析为 MC 组件格式 (JSON Array)。");
                        return element;
                    }
                }
            } catch (JsonSyntaxException e) {
                if (criteria.startsWith("{")) {
                    logger.warn("检测到前缀尝试使用 JSON 格式但语法错误: {}", e.getMessage());
                }
            }
        }

        logger.info("消息前缀将采用默认风格的文本前缀: {}", messagePrefixText);
        JsonObject obj = new JsonObject();
        obj.addProperty("text", messagePrefixText);
        obj.addProperty("color", "yellow");
        return obj;
    }

    public boolean isTranslationEnabled() {
        LanguageService service = languageService;
        return service != null && service.isInternalEnable();
    }

    public String translate(String key, String[] args) {
        LanguageService service = languageService;
        if (service == null) {
            return key;
        }
        return service.translate(key, args);
    }

    /**
     * @return 配置运行时状态（<b>唯一</b>配置状态来源）
     */
    public Config getConfig() {
        return config;
    }

    /**
     * 替换运行时配置状态（供测试与工具注入；正常流程由 {@code loadConfig()} 完成）
     *
     * @param config 新的配置运行时状态
     */
    public void setConfig(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config 不能为 null");
        }
        this.config = config;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    public WebsocketManager getWebsocketManager() {
        return websocketManager;
    }

    public HandleApiService getHandleApiService() {
        return handleApiService;
    }

    public HandleCommandReturnMessageService getHandleCommandReturnMessageService() {
        return handleCommandReturnMessageService;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getServerType() {
        return serverType;
    }

    public Gson getGson() {
        return gson;
    }

    public JsonElement getMessagePrefixJsonElement() {
        return messagePrefixJsonElement;
    }
}
