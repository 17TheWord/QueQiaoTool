package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.constant.CommandConstantMessage;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GlobalContext {
    private static Config config;
    private static Logger logger;
    private static WebsocketManager websocketManager;
    private static HandleApiService handleApiService;
    private static HandleCommandReturnMessageService handleCommandReturnMessageService;
    private static String serverVersion;
    private static String serverType;
    private static RconClient rconClient;

    public static void init(boolean isModServer, String serverVersion, String serverType, HandleApiService handleApiImpl, HandleCommandReturnMessageService handleCommandReturnMessageImpl) {
        logger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        logger.info(BaseConstant.LAUNCHING);
        config = Config.loadConfig(isModServer, logger);
        GlobalContext.serverVersion = serverVersion;
        GlobalContext.serverType = serverType;
        handleApiService = handleApiImpl;
        handleCommandReturnMessageService = handleCommandReturnMessageImpl;
        logger.info(BaseConstant.INITIALIZED);

        initWebsocketManager();
        initRconClient();
    }

    /**
     * 执行全局重载命令
     * <p> 1. 先重新读取配置文件 </p>
     * <p> 2. 再重新连接所有 Websocket Client </p>
     * <p> 3. 重连Rcon </p>
     *
     * @param commandReturner 命令返回者
     * @param isModServer     是否为模组服
     */
    public static void executeReloadCommand(Object commandReturner, boolean isModServer) {
        setConfig(Config.loadConfig(isModServer, logger));
        handleCommandReturnMessageService.sendReturnMessage(commandReturner, CommandConstantMessage.RELOAD_CONFIG);
        websocketManager.restartWebsocket(commandReturner);

        rconClient.stop();
        rconClient.setPort(config.getRcon().getPort());
        rconClient.setPassword(config.getRcon().getPassword());
        rconClient.connect();
    }

    public static void shutdown() {
        websocketManager.stopWebsocketByServerClose();
        if (config.getRcon().isEnable()) rconClient.stop();
        logger.info("鹊桥已关闭");
    }

    //
    // Websocket
    //
    private static void initWebsocketManager() {
        websocketManager = new WebsocketManager();
        websocketManager.startWebsocket(null);
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
     * 发送 Rcon 命令
     *
     * @param command 命令
     * @return 命令返回结果
     */
    public static String sendRconCommand(String command) throws IOException {
        if (config.getRcon().isEnable()) {
            return rconClient.sendCommand(command);
        } else {
            logger.warn("Rcon 未启用，无法发送命令");
            return "Rcon 未启用，无法发送命令";
        }
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
}
