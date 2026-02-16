package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.localize.LanguageService;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.github.theword.queqiao.tool.utils.ServerStatusCollector;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GlobalContext {
    private static Config config;
    private static Logger logger;
    private static Gson gson;
    private static WebsocketManager websocketManager;
    private static HandleApiService handleApiService;
    private static HandleCommandReturnMessageService handleCommandReturnMessageService;
    private static String serverVersion;
    private static String serverType;
    private static boolean isModServer;
    private static RconClient rconClient;
    private static JsonElement messagePrefixJsonElement;
    private static LanguageService languageService;

    public static void init(boolean isModServer, String serverVersion, String serverType, HandleApiService handleApiImpl, HandleCommandReturnMessageService handleCommandReturnMessageImpl) {
        GlobalContext.isModServer = isModServer;
        logger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        logger.info(BaseConstant.LAUNCHING);
        gson = GsonUtils.getGson();
        config = Config.loadConfig(isModServer, logger);
        GlobalContext.serverVersion = serverVersion;
        GlobalContext.serverType = serverType;
        handleApiService = handleApiImpl;
        handleCommandReturnMessageService = handleCommandReturnMessageImpl;
        logger.info(BaseConstant.INITIALIZED);

        messagePrefixJsonElement = initMessagePrefixJsonObject(config.getMessagePrefix());
        languageService = new LanguageService(isModServer, logger);
        ServerStatusCollector.initPingTarget(logger);
        initWebsocketManager();
        initRconClient();
    }

    /**
     * 执行全局重载命令
     * <p> 1. 先重新读取配置文件 </p>
     * <p> 2. 加载翻译功能 </p>
     * <p> 3. 再重新连接所有 Websocket Client </p>
     * <p> 4. 重连 Rcon </p>
     *
     * @param commandReturner 命令返回者
     */
    public static void executeReloadCommand(Object commandReturner) {
        setConfig(Config.loadConfig(isModServer, logger));
        messagePrefixJsonElement = initMessagePrefixJsonObject(config.getMessagePrefix());
        languageService.reload();
        ServerStatusCollector.initPingTarget(logger);
        websocketManager.restart(commandReturner);
        restartRconClient();
        handleCommandReturnMessageService.sendReturnMessage(commandReturner, CommandConstant.RELOAD_CONFIG);
    }

    /**
     * 关闭鹊桥
     */
    public static void shutdown() {
        websocketManager.stop(1000, WebsocketConstantMessage.Client.CLOSING_CONNECTION, null);
        if (config.getRcon().isEnable() && rconClient != null) rconClient.stop();
        languageService.disable();
        logger.info("鹊桥已关闭");
    }

    //
    // Websocket
    //
    private static void initWebsocketManager() {
        websocketManager = new WebsocketManager(logger, gson, handleCommandReturnMessageService);
        websocketManager.start(null);
    }

    /**
     * 同时向所有已连接的 WebSocket 客户端和服务端广播事件
     *
     * @param baseEvent 事件对象
     */
    public static void sendEvent(BaseEvent baseEvent) {
        websocketManager.sendEvent(baseEvent);
    }

    //
    // Rcon
    //
    private static void initRconClient() {
        if (config.getRcon().isEnable()) {
            rconClient = new RconClient(logger, config.getRcon().getPort(), config.getRcon().getPassword());
            rconClient.connect();
        } else {
            logger.info("Rcon 未启用，跳过 Rcon 客户端初始化");
        }
    }

    /**
     * 重启 Rcon 客户端
     * <p> 1. 先判断 Rcon 是否启用 </p>
     * <p> 2. 调用 rconClient.stop() </p>
     * <p> 3. 重新设置端口和密码 </p>
     * <p> 4. 调用 rconClient.connect() </p>
     * <p> 5. 若未启用则打印日志并跳过 </p>
     */
    private static void restartRconClient() {
        // 1. 如果新配置关闭了 Rcon
        if (!config.getRcon().isEnable()) {
            if (rconClient != null) {
                rconClient.stop();
                rconClient = null; // 必须置 null，否则 sendRconCommand 的判断会失效
                logger.info("Rcon 已根据新配置禁用并关闭连接");
            }
            return;
        }

        // 2. 如果开启了 Rcon
        if (rconClient == null) {
            // 之前没开，现在开了：初始化
            initRconClient();
        } else {
            // 之前开着，现在依然开着：执行重启（更新端口/密码）
            rconClient.stop();
            rconClient.setPort(config.getRcon().getPort());
            rconClient.setPassword(config.getRcon().getPassword());
            rconClient.connect();
        }
    }

    /**
     * 发送 Rcon 命令
     *
     * @param command 命令
     * @return 命令返回结果
     */
    public static String sendRconCommand(String command) throws IOException {
        if (!config.getRcon().isEnable()) {
            throw new IOException("Rcon 功能未在配置文件中启用");
        }
        if (rconClient == null || !rconClient.isConnected()) {
            throw new IOException("Rcon 客户端未连接或已关闭");
        }
        return rconClient.sendCommand(command);
    }

    //
    // Message Prefix
    //

    /**
     * 初始化消息前缀 JsonObject
     *
     * <p> 自 0.6.0 开始，支持对 "" 前缀的处理，对 Json 更加智能的处理 </p>
     *
     * @return JsonElement 消息前缀
     * @since 0.6.0
     */
    public static JsonElement initMessagePrefixJsonObject(String messagePrefixText) {
        if (messagePrefixText == null) messagePrefixText = "[鹊桥]";

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
                    if (!array.isEmpty() && array.get(0).isJsonObject()) {
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

    //
    // Language Service
    //
    public static boolean isTranslationEnabled() {
        return languageService.isInternalEnable();
    }

    /**
     * 翻译指定键值
     *
     * @param key  翻译键值
     * @param args 翻译参数
     * @return 翻译结果
     * @since 0.6.0
     */
    public static String translate(String key, String[] args) {
        return languageService.translate(key, args);
    }

    //
    // Getters and Setters
    //
    public static Config getConfig() {
        return config;
    }

    public static void setConfig(Config config) {
        GlobalContext.config = config;
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        GlobalContext.logger = logger;
    }

    public static WebsocketManager getWebsocketManager() {
        return websocketManager;
    }

    public static HandleApiService getHandleApiService() {
        return handleApiService;
    }

    public static HandleCommandReturnMessageService getHandleCommandReturnMessageService() {
        return handleCommandReturnMessageService;
    }

    public static String getServerVersion() {
        return serverVersion;
    }

    public static String getServerType() {
        return serverType;
    }

    public static Gson getGson() {
        return gson;
    }

    /**
     * 获取消息前缀的 JsonElement
     *
     * <p> 自 0.6.0 开始，返回类型从 {@code JsonObject} 改为 {@link JsonElement} </p>
     *
     * @return JsonElement 消息前缀
     * @since 0.6.0
     */
    public static JsonElement getMessagePrefixJsonObject() {
        return messagePrefixJsonElement;
    }
}
