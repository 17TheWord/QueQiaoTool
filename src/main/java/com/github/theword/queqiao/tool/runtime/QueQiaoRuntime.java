package com.github.theword.queqiao.tool.runtime;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.localize.LanguageService;
import com.github.theword.queqiao.tool.protocol.handler.status.ServerStatusCollector;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueQiaoRuntime {
    private Config config;
    private Logger logger;
    private Gson gson;
    private WebsocketManager websocketManager;
    private HandleApiService handleApiService;
    private HandleCommandReturnMessageService handleCommandReturnMessageService;
    private String serverVersion;
    private String serverType;
    private boolean modServer;
    private RconClient rconClient;
    private JsonElement messagePrefixJsonElement;
    private LanguageService languageService;

    private QueQiaoRuntime() {
    }

    public static QueQiaoRuntime empty() {
        return new QueQiaoRuntime();
    }

    public static QueQiaoRuntime create(boolean modServer, String serverVersion, String serverType, HandleApiService handleApiService, HandleCommandReturnMessageService handleCommandReturnMessageService) {
        QueQiaoRuntime runtime = new QueQiaoRuntime();
        runtime.modServer = modServer;
        runtime.serverVersion = serverVersion;
        runtime.serverType = serverType;
        runtime.handleApiService = handleApiService;
        runtime.handleCommandReturnMessageService = handleCommandReturnMessageService;
        runtime.logger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        runtime.gson = GsonUtils.getGson();
        runtime.config = Config.loadConfig(modServer, runtime.logger);
        return runtime;
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
        languageService.reload();
        ServerStatusCollector.initPingTarget(logger);
        websocketManager.restart(commandReturner);
        restartRconClient();
        handleCommandReturnMessageService.sendReturnMessage(commandReturner, CommandConstant.RELOAD_CONFIG);
    }

    public void shutdown() {
        websocketManager.stop(1000, WebsocketConstantMessage.Client.CLOSING_CONNECTION, null);
        if (config.getRcon().isEnable() && rconClient != null) {
            rconClient.stop();
        }
        languageService.disable();
        logger.info("鹊桥已关闭");
    }

    public void sendEvent(BaseEvent baseEvent) {
        websocketManager.sendEvent(baseEvent);
    }

    private void initWebsocketManager() {
        websocketManager = new WebsocketManager(logger, gson, handleCommandReturnMessageService);
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
        return languageService.isInternalEnable();
    }

    public String translate(String key, String[] args) {
        return languageService.translate(key, args);
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
