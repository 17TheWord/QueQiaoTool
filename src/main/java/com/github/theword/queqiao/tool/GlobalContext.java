package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.utils.GsonUtils;
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
    private static JsonObject messagePrefixJsonObject;

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

        messagePrefixJsonObject = initMessagePrefixJsonObject(config.getMessagePrefix());
        initWebsocketManager();
        initRconClient();
    }

    /**
     * 执行全局重载命令
     * <p> 1. 先重新读取配置文件 </p>
     * <p> 2. 再重新连接所有 Websocket Client </p>
     * <p> 3. 重连 Rcon </p>
     *
     * @param commandReturner 命令返回者
     */
    public static void executeReloadCommand(Object commandReturner) {
        setConfig(Config.loadConfig(isModServer, logger));
        handleCommandReturnMessageService.sendReturnMessage(commandReturner, CommandConstant.RELOAD_CONFIG);
        messagePrefixJsonObject = initMessagePrefixJsonObject(config.getMessagePrefix());
        websocketManager.restart(commandReturner);
        restartRconClient();
    }

    /**
     * 关闭鹊桥
     */
    public static void shutdown() {
        websocketManager.stop(1000, WebsocketConstantMessage.Client.CLOSING_CONNECTION, null);
        if (config.getRcon().isEnable() && rconClient != null) rconClient.stop();
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
        if (rconClient == null) {
            initRconClient();
        } else {
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
            throw new IOException("Rcon 未启用，无法发送命令");
        } else if (rconClient == null) {
            throw new IOException("Rcon 未启用，无法发送命令");
        } else if (!rconClient.isConnected()) {
            throw new IOException("Rcon 未启用，无法发送命令");
        } else {
            return rconClient.sendCommand(command);
        }
    }

    //
    // Message Prefix
    //

    /**
     * 初始化消息前缀 JsonObject
     * 
     * @return JsonObject 消息前缀
     * @since 0.4.2
     */
    public static JsonObject initMessagePrefixJsonObject(String messagePrefixText) {
        if (messagePrefixText == null || messagePrefixText.isEmpty()) {
            messagePrefixText = "[鹊桥]";
        }
        try {
            JsonElement element = gson.fromJson(messagePrefixText, JsonElement.class);
            if (element.isJsonObject()) {
                logger.info("消息前缀 {} 符合mc消息组件格式，将采用自定义风格的消息前缀", messagePrefixText);
                return element.getAsJsonObject();
            }
            logger.info("消息前缀 {} 不是合法的 JSON 对象，将使用默认风格的自定义文本前缀", messagePrefixText);

        } catch (JsonSyntaxException e) {
            logger.info("消息前缀 {} 未采用自定义风格，将使用默认风格的自定义文本前缀", messagePrefixText);
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("text", messagePrefixText);
        obj.addProperty("color", "yellow");
        return obj;
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
     * 获取消息前缀 JsonObject
     *
     * @since 0.4.2
     * @return JsonObject 消息前缀
     */
    public static JsonObject getMessagePrefixJsonObject() {
        return messagePrefixJsonObject;
    }
}
