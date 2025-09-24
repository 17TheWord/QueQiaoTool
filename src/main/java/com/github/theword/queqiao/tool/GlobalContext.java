package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalContext {
    private static Config config;
    private static Logger logger;
    private static WebsocketManager websocketManager;
    private static HandleApiService handleApiService;
    private static HandleProtocolMessage handleProtocolMessage;
    private static HandleCommandReturnMessageService handleCommandReturnMessageService;
    private static String serverVersion;
    private static String serverType;

    public static void init(
            boolean isModServer,
            String serverVersion,
            String serverType,
            HandleApiService handleApiImpl,
            HandleCommandReturnMessageService handleCommandReturnMessageImpl
    ) {
        logger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        logger.info(BaseConstant.LAUNCHING);
        config = Config.loadConfig(isModServer);
        GlobalContext.serverVersion = serverVersion;
        GlobalContext.serverType = serverType;
        websocketManager = new WebsocketManager();
        handleApiService = handleApiImpl;
        handleProtocolMessage = new HandleProtocolMessage();
        handleCommandReturnMessageService = handleCommandReturnMessageImpl;
        logger.info(BaseConstant.INITIALIZED);
    }

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

    public static HandleProtocolMessage getHandleProtocolMessage() {
        return handleProtocolMessage;
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
