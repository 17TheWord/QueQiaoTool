package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.runtime.QueQiaoRuntime;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;

public final class GlobalContext {
    private static QueQiaoRuntime runtime = QueQiaoRuntime.empty();

    private GlobalContext() {
    }

    public static void init(boolean isModServer, String serverVersion, String serverType, HandleApiService handleApiImpl, HandleCommandReturnMessageService handleCommandReturnMessageImpl) {
        QueQiaoRuntime newRuntime = QueQiaoRuntime.create(isModServer, serverVersion, serverType, handleApiImpl, handleCommandReturnMessageImpl);
        runtime = newRuntime;
        newRuntime.start();
    }

    public static void executeReloadCommand(Object commandReturner) {
        runtime.reload(commandReturner);
    }

    public static void shutdown() {
        runtime.shutdown();
    }

    public static void sendEvent(BaseEvent baseEvent) {
        runtime.sendEvent(baseEvent);
    }

    public static String sendRconCommand(String command) throws RconException {
        return runtime.sendRconCommand(command);
    }

    public static JsonElement initMessagePrefixJsonObject(String messagePrefixText) {
        return runtime.initMessagePrefixJsonObject(messagePrefixText);
    }

    public static boolean isTranslationEnabled() {
        return runtime.isTranslationEnabled();
    }

    public static String translate(String key, String[] args) {
        return runtime.translate(key, args);
    }

    public static Config getConfig() {
        return runtime.getConfig();
    }

    public static void setConfig(Config config) {
        runtime.setConfig(config);
    }

    public static Logger getLogger() {
        return runtime.getLogger();
    }

    public static void setLogger(Logger logger) {
        runtime.setLogger(logger);
    }

    public static WebsocketManager getWebsocketManager() {
        return runtime.getWebsocketManager();
    }

    public static HandleApiService getHandleApiService() {
        return runtime.getHandleApiService();
    }

    public static HandleCommandReturnMessageService getHandleCommandReturnMessageService() {
        return runtime.getHandleCommandReturnMessageService();
    }

    public static String getServerVersion() {
        return runtime.getServerVersion();
    }

    public static String getServerType() {
        return runtime.getServerType();
    }

    public static Gson getGson() {
        return runtime.getGson();
    }

    public static JsonElement getMessagePrefixJsonObject() {
        return runtime.getMessagePrefixJsonElement();
    }
}
