package com.github.theword.queqiao.tool.runtime;

import com.github.theword.queqiao.tool.config.Config;
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

    private volatile Config config;

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

    private volatile RconClient rconClient;

    private volatile JsonElement messagePrefixJsonElement;

    private volatile LanguageService languageService;

    private QueQiaoRuntime(
            boolean modServer,
            String serverVersion,
            String serverType,
            HandleApiService handleApiService,
            HandleCommandReturnMessageService handleCommandReturnMessageService,
            Logger logger,
            Config config) {
        this.modServer = modServer;
        this.serverVersion = serverVersion;
        this.serverType = serverType;
        this.handleApiService = handleApiService;
        this.handleCommandReturnMessageService = handleCommandReturnMessageService;
        this.logger = logger;
        this.gson = GsonUtils.getGson();
        this.config = config;
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
                NOP_LOGGER,
                Config.defaults(NOP_LOGGER)
        );
    }

    public static QueQiaoRuntime create(boolean modServer, String serverVersion, String serverType, HandleApiService handleApiService, HandleCommandReturnMessageService handleCommandReturnMessageService) {
        Logger runtimeLogger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        return new QueQiaoRuntime(
                modServer,
                serverVersion,
                serverType,
                handleApiService,
                handleCommandReturnMessageService,
                runtimeLogger,
                Config.loadConfig(modServer, runtimeLogger)
        );
    }

    public void start() {
        logger.info(BaseConstant.LAUNCHING);
        logger.info(BaseConstant.INITIALIZED);

        messagePrefixJsonElement = initMessagePrefixJsonObject(config.getMessagePrefix());
        languageService = new LanguageService(modServer, logger);
        ServerStatusCollector.initPingTarget(logger);
        initWebsocketManager();
        initRconClient();
    }

    public void reload(Object commandReturner) {
        setConfig(Config.loadConfig(modServer, logger));
        messagePrefixJsonElement = initMessagePrefixJsonObject(config.getMessagePrefix());
        LanguageService service = languageService;
        if (service != null) {
            service.reload();
        }
        ServerStatusCollector.initPingTarget(logger);
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
        WebsocketManager manager = websocketManager;
        if (manager != null) {
            manager.stop(1000, WebsocketConstantMessage.SHUTDOWN, null);
            websocketManager = null;
        }

        RconClient client = rconClient;
        if (client != null) {
            client.stop();
            rconClient = null;
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
        baseEvent.fillServerContext(config.getServerName(), serverVersion, serverType);
        manager.sendEvent(baseEvent);
    }

    private void initWebsocketManager() {
        websocketManager = new WebsocketManager(logger, gson, handleCommandReturnMessageService, handleProtocolMessage, config);
        websocketManager.start(null);
    }

    private void initRconClient() {
        if (config.getRcon().isEnable()) {
            rconClient = new RconClient(logger, config.getRcon().getPort(), config.getRcon().getPassword());
            rconClient.connect();
        } else {
            logger.info("Rcon 未启用，跳过 Rcon 客户端初始化");
        }
    }

    private void restartRconClient() {
        if (!config.getRcon().isEnable()) {
            if (rconClient != null) {
                rconClient.stop();
                rconClient = null;
                logger.info("Rcon 已根据新配置禁用并关闭连接");
            }
            return;
        }

        if (rconClient == null) {
            initRconClient();
        } else {
            rconClient.stop();
            rconClient.setPort(config.getRcon().getPort());
            rconClient.setPassword(config.getRcon().getPassword());
            rconClient.connect();
        }
    }

    public String sendRconCommand(String command) throws RconException {
        if (!config.getRcon().isEnable()) {
            throw RconException.disabled();
        }
        if (rconClient == null || !rconClient.isConnected()) {
            throw RconException.disconnected();
        }
        return rconClient.sendCommand(command);
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

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
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
