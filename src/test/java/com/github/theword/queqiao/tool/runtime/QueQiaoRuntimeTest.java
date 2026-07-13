package com.github.theword.queqiao.tool.runtime;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.RconConfig;
import com.github.theword.queqiao.tool.constant.CommandConstant;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.rcon.RconClient;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueQiaoRuntimeTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void createShouldInitializeRuntimeStateWithoutStartingServices() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("[prefix]", false));
        FakeHandleApiService apiService = new FakeHandleApiService();
        FakeCommandReturnService commandReturnService = new FakeCommandReturnService();

        QueQiaoRuntime runtime = createRuntime(dependencies, apiService, commandReturnService);

        assertNotNull(runtime.getConfig());
        assertNotNull(runtime.getLogger());
        assertNotNull(runtime.getGson());
        assertSame(apiService, runtime.getHandleApiService());
        assertSame(commandReturnService, runtime.getHandleCommandReturnMessageService());
        assertEquals("1.20.1", runtime.getServerVersion());
        assertEquals("fabric", runtime.getServerType());
        assertNull(runtime.getWebsocketManager());
        assertNull(dependencies.websocketManager);
        assertNull(dependencies.languageService);
        assertNull(dependencies.rconClient);
    }

    @Test
    void emptyShouldAllowLegacySetters() {
        QueQiaoRuntime runtime = QueQiaoRuntime.empty();
        Config config = config("[prefix]", false);

        runtime.setConfig(config);
        runtime.setLogger(logger);

        assertSame(config, runtime.getConfig());
        assertSame(logger, runtime.getLogger());
    }

    @Test
    void initMessagePrefixJsonObjectShouldHandleSupportedFormats() {
        QueQiaoRuntime runtime = createRuntime(new FakeRuntimeDependencies(config("[prefix]", false)));

        JsonElement nullPrefix = runtime.initMessagePrefixJsonObject(null);
        assertTrue(nullPrefix.isJsonObject());
        assertTrue(nullPrefix.getAsJsonObject().has("text"));

        JsonElement emptyPrefix = runtime.initMessagePrefixJsonObject("   ");
        assertEquals("", emptyPrefix.getAsJsonObject().get("text").getAsString());
        assertFalse(emptyPrefix.getAsJsonObject().has("color"));

        JsonObject textPrefix = runtime.initMessagePrefixJsonObject("QueQiao").getAsJsonObject();
        assertEquals("QueQiao", textPrefix.get("text").getAsString());
        assertEquals("yellow", textPrefix.get("color").getAsString());

        JsonObject objectPrefix = runtime.initMessagePrefixJsonObject("{\"text\":\"QQ\",\"color\":\"green\"}").getAsJsonObject();
        assertEquals("QQ", objectPrefix.get("text").getAsString());
        assertEquals("green", objectPrefix.get("color").getAsString());

        JsonElement arrayPrefix = runtime.initMessagePrefixJsonObject("[{\"text\":\"QQ\"}]");
        assertTrue(arrayPrefix.isJsonArray());
        assertEquals("QQ", arrayPrefix.getAsJsonArray().get(0).getAsJsonObject().get("text").getAsString());

        JsonObject invalidJsonPrefix = runtime.initMessagePrefixJsonObject("{bad-json}").getAsJsonObject();
        assertEquals("{bad-json}", invalidJsonPrefix.get("text").getAsString());
        assertEquals("yellow", invalidJsonPrefix.get("color").getAsString());
    }

    @Test
    void startShouldInitializeLifecycleCollaboratorsAndSkipDisabledRcon() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("[prefix]", false));
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();

        assertTrue(runtime.getMessagePrefixJsonElement().isJsonObject());
        assertNotNull(dependencies.websocketManager);
        assertEquals(1, dependencies.websocketManager.startCount);
        assertNotNull(dependencies.languageService);
        assertEquals(0, dependencies.languageService.reloadCount);
        assertEquals(1, dependencies.pingInitCount);
        assertNull(dependencies.rconClient);
    }

    @Test
    void startShouldConnectEnabledRcon() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("[prefix]", true, 25580, "secret"));
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();

        assertNotNull(dependencies.rconClient);
        assertEquals(25580, dependencies.rconClient.port);
        assertEquals("secret", dependencies.rconClient.password);
        assertEquals(1, dependencies.rconClient.connectCount);
    }

    @Test
    void reloadShouldReplaceConfigRestartServicesAndReportResult() {
        Config firstConfig = config("old", false);
        Config secondConfig = config("{\"text\":\"new\",\"color\":\"blue\"}", true, 25581, "second");
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(firstConfig, secondConfig);
        FakeCommandReturnService commandReturnService = new FakeCommandReturnService();
        QueQiaoRuntime runtime = createRuntime(dependencies, new FakeHandleApiService(), commandReturnService);
        Object commandReturner = new Object();

        runtime.start();
        runtime.reload(commandReturner);

        assertSame(secondConfig, runtime.getConfig());
        assertEquals("new", runtime.getMessagePrefixJsonElement().getAsJsonObject().get("text").getAsString());
        assertEquals(1, dependencies.languageService.reloadCount);
        assertEquals(2, dependencies.pingInitCount);
        assertEquals(1, dependencies.websocketManager.restartCount);
        assertSame(commandReturner, dependencies.websocketManager.restartCommandReturner);
        assertNotNull(dependencies.rconClient);
        assertEquals(1, dependencies.rconClient.connectCount);
        assertEquals(Arrays.asList(CommandConstant.RELOAD_CONFIG), commandReturnService.messages);
    }

    @Test
    void reloadShouldStopRconWhenNewConfigDisablesIt() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(
                config("old", true, 25580, "first"),
                config("new", false)
        );
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();
        FakeRconClient rconClient = dependencies.rconClient;
        runtime.reload(new Object());

        assertEquals(1, rconClient.stopCount);
        assertEquals(1, rconClient.connectCount);
    }

    @Test
    void reloadShouldReconnectExistingEnabledRconWithNewSettings() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(
                config("old", true, 25580, "first"),
                config("new", true, 25581, "second")
        );
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();
        FakeRconClient rconClient = dependencies.rconClient;
        runtime.reload(new Object());

        assertSame(rconClient, dependencies.rconClient);
        assertEquals(1, rconClient.stopCount);
        assertEquals(2, rconClient.connectCount);
        assertEquals(25581, rconClient.port);
        assertEquals("second", rconClient.password);
    }

    @Test
    void shutdownShouldStopRuntimeResources() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("prefix", true, 25580, "secret"));
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();
        runtime.shutdown();

        assertEquals(1, dependencies.websocketManager.stopCount);
        assertEquals(1000, dependencies.websocketManager.stopCode);
        assertEquals(WebsocketConstantMessage.Client.CLOSING_CONNECTION, dependencies.websocketManager.stopReason);
        assertEquals(1, dependencies.rconClient.stopCount);
        assertEquals(1, dependencies.languageService.disableCount);
    }

    @Test
    void sendRconCommandShouldValidateRconStateAndDelegateWhenConnected() throws RconException {
        QueQiaoRuntime disabledRuntime = createRuntime(new FakeRuntimeDependencies(config("prefix", false)));
        assertThrows(RconException.class, () -> disabledRuntime.sendRconCommand("list"));

        QueQiaoRuntime disconnectedRuntime = createRuntime(new FakeRuntimeDependencies(config("prefix", true)));
        assertThrows(RconException.class, () -> disconnectedRuntime.sendRconCommand("list"));

        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("prefix", true));
        QueQiaoRuntime runtime = createRuntime(dependencies);
        runtime.start();

        assertEquals("ok:list", runtime.sendRconCommand("list"));
        assertEquals("list", dependencies.rconClient.lastCommand);
    }

    @Test
    void sendEventShouldDelegateToWebsocketManager() {
        FakeRuntimeDependencies dependencies = new FakeRuntimeDependencies(config("prefix", false));
        QueQiaoRuntime runtime = createRuntime(dependencies);

        runtime.start();
        runtime.sendEvent(null);

        assertEquals(1, dependencies.websocketManager.sendEventCount);
    }

    private QueQiaoRuntime createRuntime(FakeRuntimeDependencies dependencies) {
        return createRuntime(dependencies, new FakeHandleApiService(), new FakeCommandReturnService());
    }

    private QueQiaoRuntime createRuntime(FakeRuntimeDependencies dependencies, HandleApiService handleApiService, HandleCommandReturnMessageService commandReturnService) {
        return QueQiaoRuntime.create(false, "1.20.1", "fabric", handleApiService, commandReturnService, dependencies);
    }

    private FakeConfig config(String messagePrefix, boolean rconEnabled) {
        return config(messagePrefix, rconEnabled, 25575, "");
    }

    private FakeConfig config(String messagePrefix, boolean rconEnabled, int rconPort, String rconPassword) {
        FakeConfig config = new FakeConfig(logger);
        config.setMessagePrefix(messagePrefix);
        RconConfig rcon = new RconConfig();
        rcon.setEnable(rconEnabled);
        rcon.setPort(rconPort);
        rcon.setPassword(rconPassword);
        config.setRcon(rcon);
        return config;
    }

    private static final class FakeRuntimeDependencies extends QueQiaoRuntime.RuntimeDependencies {
        private final List<Config> configs;
        private int configIndex;
        private int pingInitCount;
        private FakeWebsocketManager websocketManager;
        private FakeLanguageService languageService;
        private FakeRconClient rconClient;

        private FakeRuntimeDependencies(Config... configs) {
            this.configs = new ArrayList<>(Arrays.asList(configs));
        }

        @Override
        Config loadConfig(boolean modServer, Logger logger) {
            Config config = configs.get(Math.min(configIndex, configs.size() - 1));
            configIndex++;
            return config;
        }

        @Override
        WebsocketManager createWebsocketManager(Logger logger, Gson gson, HandleCommandReturnMessageService handleCommandReturnMessageService) {
            websocketManager = new FakeWebsocketManager(logger, gson, handleCommandReturnMessageService);
            return websocketManager;
        }

        @Override
        QueQiaoRuntime.RuntimeLanguageService createLanguageService(boolean modServer, Logger logger) {
            languageService = new FakeLanguageService();
            return languageService;
        }

        @Override
        RconClient createRconClient(Logger logger, int port, String password) {
            rconClient = new FakeRconClient(logger, port, password);
            return rconClient;
        }

        @Override
        void initPingTarget(Logger logger) {
            pingInitCount++;
        }
    }

    private static final class FakeConfig extends Config {
        private FakeConfig(Logger logger) {
            super(logger);
        }
    }

    private static final class FakeWebsocketManager extends WebsocketManager {
        private int startCount;
        private int restartCount;
        private int stopCount;
        private int stopCode;
        private String stopReason;
        private Object restartCommandReturner;
        private int sendEventCount;

        private FakeWebsocketManager(Logger logger, Gson gson, HandleCommandReturnMessageService handleCommandReturnMessageService) {
            super(logger, gson, handleCommandReturnMessageService);
        }

        @Override
        public void start(Object commandReturner) {
            startCount++;
        }

        @Override
        public void restart(Object commandReturner) {
            restartCount++;
            restartCommandReturner = commandReturner;
        }

        @Override
        public void stop(int code, String reason, Object commandReturner) {
            stopCount++;
            stopCode = code;
            stopReason = reason;
        }

        @Override
        public void sendEvent(com.github.theword.queqiao.tool.event.base.BaseEvent event) {
            sendEventCount++;
        }
    }

    private static final class FakeLanguageService implements QueQiaoRuntime.RuntimeLanguageService {
        private int reloadCount;
        private int disableCount;

        @Override
        public void reload() {
            reloadCount++;
        }

        @Override
        public void disable() {
            disableCount++;
        }

        @Override
        public boolean isInternalEnable() {
            return true;
        }

        @Override
        public String translate(String key, Object[] args) {
            return key;
        }
    }

    private static final class FakeRconClient extends RconClient {
        private int connectCount;
        private int stopCount;
        private int port;
        private String password;
        private String lastCommand;

        private FakeRconClient(Logger logger, int port, String password) {
            super(logger, port, password);
            this.port = port;
            this.password = password;
        }

        @Override
        public void connect() {
            connectCount++;
        }

        @Override
        public void stop() {
            stopCount++;
        }

        @Override
        public boolean isConnected() {
            return connectCount > stopCount;
        }

        @Override
        public String sendCommand(String command) {
            lastCommand = command;
            return "ok:" + command;
        }

        @Override
        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public void setPassword(String password) {
            this.password = password;
        }
    }

    private static final class FakeHandleApiService implements HandleApiService {
        @Override
        public void handleBroadcastMessage(JsonElement jsonData) {
        }

        @Override
        public void handleSendTitleMessage(JsonElement titlePayload, JsonElement subTitlePayload, int fadeIn, int stay, int fadeOut) {
        }

        @Override
        public void handleSendActionBarMessage(JsonElement jsonData) {
        }

        @Override
        public PrivateMessageResponse handleSendPrivateMessage(String nickname, UUID uuid, JsonElement jsonData) {
            return null;
        }
    }

    private static final class FakeCommandReturnService extends HandleCommandReturnMessageService {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void handleCommandReturnMessage(Object commandReturner, String message) {
            messages.add(message);
        }

        @Override
        public boolean hasPermission(Object commandReturner, String permissionNode) {
            return true;
        }
    }
}
