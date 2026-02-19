package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

/**
 * 配置项 服务器初始化阶段请调用 {@link #loadConfig(boolean, Logger)} 方法加载配置文件
 */
public class Config extends CommonConfig {
    private static final String CONFIG_FILE_NAME = "config.yml";

    /**
     * 配置目录（config 或 plugins）
     */
    private final String configFolder;

    /**
     * 是否启用插件/模组
     */
    private boolean enable = true;

    /**
     * 是否开启调试模式
     *
     * <p>对详部分简写日志进行 GlobalContext.getLogger().info 输出
     */
    private boolean debug = false;

    /**
     * 服务器名
     */
    private String serverName = "Server";

    /**
     * 访问令牌
     */
    private String accessToken = "";

    /**
     * 消息前缀
     */
    private String messagePrefix = "[鹊桥]";

    /**
     * 是否开启翻译
     */
    private boolean enableTranslation = false;

    /**
     * 忽略的命令列表
     */
    private Set<String> ignoredCommands;

    /**
     * WebSocket Server 配置项
     */
    private WebSocketServerConfig websocketServer = new WebSocketServerConfig();

    /**
     * WebSocket Client 配置项
     */
    private WebSocketClientConfig websocketClient = new WebSocketClientConfig();

    /**
     * 订阅事件配置项
     */
    private SubscribeEventConfig subscribeEvent = new SubscribeEventConfig();

    /**
     * Rcon 客户端配置项
     */
    private RconConfig rcon = new RconConfig();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getMessagePrefix() {
        return messagePrefix;
    }

    public Set<String> getIgnoredCommands() {
        return ignoredCommands;
    }

    public void setIgnoredCommands(Set<String> ignoredCommands) {
        this.ignoredCommands = ignoredCommands;
    }

    public void setMessagePrefix(String messagePrefix) {
        this.messagePrefix = messagePrefix;
    }

    public boolean isEnableTranslation() {
        return enableTranslation;
    }

    public void setEnableTranslation(boolean enableTranslation) {
        this.enableTranslation = enableTranslation;
    }

    public WebSocketServerConfig getWebsocketServer() {
        return websocketServer;
    }

    public void setWebsocketServer(WebSocketServerConfig websocketServer) {
        this.websocketServer = websocketServer;
    }

    public WebSocketClientConfig getWebsocketClient() {
        return websocketClient;
    }

    public void setWebsocketClient(WebSocketClientConfig websocketClient) {
        this.websocketClient = websocketClient;
    }

    public SubscribeEventConfig getSubscribeEvent() {
        return subscribeEvent;
    }

    public void setSubscribeEvent(SubscribeEventConfig subscribeEvent) {
        this.subscribeEvent = subscribeEvent;
    }

    public RconConfig getRcon() {
        return rcon;
    }

    public void setRcon(RconConfig rcon) {
        this.rcon = rcon;
    }

    /**
     * Contractor
     *
     * @param isModServer 是否为模组服务端
     * @param logger      日志实现
     */
    public Config(boolean isModServer, Logger logger) {
        super(logger);
        this.configFolder = isModServer ? "config" : "plugins";
        String serverType = isModServer ? "模组" : "插件";
        logger.info("当前服务端类型为：{}服", serverType);
        readConfigFile(this.configFolder, CONFIG_FILE_NAME);
    }

    /**
     * 加载配置文件
     *
     * <p>服务端启动、初始化模组时调用
     *
     * @param isModServer 是否为模组服务端
     * @param logger      日志实现
     * @return Config
     */
    public static Config loadConfig(boolean isModServer, Logger logger) {
        return new Config(isModServer, logger);
    }

    /**
     * 对外：读取整个配置内容。
     *
     * @return 配置 Map
     */
    public synchronized Map<String, Object> readAllConfig() {
        return readConfigMap(resolveConfigPath(), CONFIG_FILE_NAME);
    }

    /**
     * 对外：按键路径读取配置项。
     *
     * <p>键路径示例：websocket_server.port</p>
     *
     * @param keyPath 键路径
     * @return 配置值，不存在时返回 null
     */
    public synchronized Object readConfig(String keyPath) {
        return readConfigValue(resolveConfigPath(), CONFIG_FILE_NAME, keyPath);
    }

    /**
     * 对外：写入整个配置内容。
     *
     * @param configMap 配置 Map
     * @return 是否写入成功
     */
    public synchronized boolean writeAllConfig(Map<String, Object> configMap) {
        boolean success = writeConfigMap(resolveConfigPath(), CONFIG_FILE_NAME, configMap);
        if (success) {
            readConfigFile(this.configFolder, CONFIG_FILE_NAME);
        }
        return success;
    }

    /**
     * 对外：按键路径写入配置项。
     *
     * <p>键路径示例：websocket_server.port</p>
     *
     * @param keyPath 键路径
     * @param value   键值
     * @return 是否写入成功
     */
    public synchronized boolean writeConfig(String keyPath, Object value) {
        boolean success = writeConfigValue(resolveConfigPath(), CONFIG_FILE_NAME, keyPath, value);
        if (success) {
            readConfigFile(this.configFolder, CONFIG_FILE_NAME);
        }
        return success;
    }

    private Path resolveConfigPath() {
        return Paths.get(this.configFolder, BaseConstant.MODULE_NAME, CONFIG_FILE_NAME);
    }

    /**
     * 加载配置文件
     *
     * @param configMap 配置文件内容
     */
    @Override
    protected void loadConfigValues(Map<String, Object> configMap) {
        enable = (boolean) configMap.get("enable");
        debug = (boolean) configMap.get("debug");
        serverName = (String) configMap.get("server_name");
        accessToken = (String) configMap.get("access_token");
        messagePrefix = (String) configMap.get("message_prefix");
        enableTranslation = (boolean) configMap.get("enable_translation");

        loadIgnoredCommands(configMap);
        loadWebsocketServerConfig(configMap);
        loadWebsocketClientConfig(configMap);
        loadSubscribeEventConfig(configMap);
        loadRconConfig(configMap);
    }

    /**
     * 加载忽略的命令列表配置项
     *
     * @param configMap ignored_commands
     */
    @SuppressWarnings("unchecked")
    private void loadIgnoredCommands(Map<String, Object> configMap) {
        if (ignoredCommands == null) {
            ignoredCommands = new HashSet<>();
        } else {
            ignoredCommands.clear();
        }
        List<String> ignoredCommandList = (List<String>) configMap.get("ignored_commands");
        if (ignoredCommandList == null) {
            super.getLogger().info("配置项 ignored_commands 为空，将只忽略默认的注册和登录命令");
        } else {
            ignoredCommands.addAll(ignoredCommandList);
            super.getLogger().info("已加载 {} 个忽略的命令", ignoredCommandList.size());
        }

        ignoredCommands.add("l");
        ignoredCommands.add("login");
        ignoredCommands.add("register");
        ignoredCommands.add("reg");
    }

    /**
     * 加载 Rcon 客户端配置项
     *
     * @param configMap Rcon
     */
    private void loadRconConfig(Map<String, Object> configMap) {
        Object rconObj = configMap.get("rcon");
        if (!(rconObj instanceof Map)) return;
        @SuppressWarnings("unchecked") Map<String, Object> rconConfig = (Map<String, Object>) rconObj;
        rcon.setEnable((Boolean) rconConfig.getOrDefault("enable", false));
        rcon.setPort((Integer) rconConfig.getOrDefault("port", 25575));
        rcon.setPassword((String) rconConfig.getOrDefault("password", ""));
    }

    /**
     * 加载 WebSocket Server 配置项
     *
     * @param configMap WebSocket Server
     */
    @SuppressWarnings("unchecked")
    private void loadWebsocketServerConfig(Map<String, Object> configMap) {
        Object websocketServerObj = configMap.get("websocket_server");
        if (!(websocketServerObj instanceof Map)) {
            super.getLogger().warn("配置项 websocket_server 缺失或格式错误，将使用默认值");
            return;
        }

        Map<String, Object> websocketServerConfig = (Map<String, Object>) websocketServerObj;
        Object enableObj = websocketServerConfig.get("enable");
        Object hostObj = websocketServerConfig.get("host");
        Object portObj = websocketServerConfig.get("port");
        Object forwardObj = websocketServerConfig.get("forward");

        websocketServer.setEnable(enableObj instanceof Boolean ? (Boolean) enableObj : true);
        websocketServer.setHost(resolveHost(hostObj, "127.0.0.1"));
        websocketServer.setPort(resolvePort(portObj, 8080, "websocket_server.port"));
        websocketServer.setForward(forwardObj instanceof Boolean && (Boolean) forwardObj);
    }

    private String resolveHost(Object hostObj, String defaultHost) {
        if (!(hostObj instanceof String)) {
            return defaultHost;
        }
        String host = ((String) hostObj).trim();
        return host.isEmpty() ? defaultHost : host;
    }

    private int resolvePort(Object portObj, int defaultPort, String key) {
        if (!(portObj instanceof Number)) {
            if (portObj != null) {
                super.getLogger().warn(
                        "配置项 {} 类型错误（{}），将使用默认值 {}", key, portObj.getClass().getSimpleName(), defaultPort
                );
            }
            return defaultPort;
        }
        int port = ((Number) portObj).intValue();
        if (port <= 0 || port > 65535) {
            super.getLogger().warn("配置项 {} 端口越界（{}），将使用默认值 {}", key, port, defaultPort);
            return defaultPort;
        }
        return port;
    }

    /**
     * 加载 WebSocket Client 配置项
     *
     * @param configMap WebSocket Client
     */
    @SuppressWarnings("unchecked")
    private void loadWebsocketClientConfig(Map<String, Object> configMap) {
        Map<String, Object> websocketClientConfig = (Map<String, Object>) configMap.get("websocket_client");
        websocketClient.setEnable((Boolean) websocketClientConfig.get("enable"));
        websocketClient.setReconnectInterval((int) websocketClientConfig.get("reconnect_interval"));
        websocketClient.setReconnectMaxTimes((int) websocketClientConfig.get("reconnect_max_times"));
        websocketClient.setUrlList((List<String>) websocketClientConfig.get("url_list"));
    }

    /**
     * 加载订阅事件配置项
     *
     * @param configMap SubscribeEvent
     */
    @SuppressWarnings("unchecked")
    private void loadSubscribeEventConfig(Map<String, Object> configMap) {
        Map<String, Object> subscribeEventConfig = (Map<String, Object>) configMap.get("subscribe_event");
        subscribeEvent.setPlayerChat((boolean) subscribeEventConfig.get("player_chat"));
        subscribeEvent.setPlayerCommand((boolean) subscribeEventConfig.get("player_command"));
        subscribeEvent.setPlayerDeath((boolean) subscribeEventConfig.get("player_death"));
        subscribeEvent.setPlayerJoin((boolean) subscribeEventConfig.get("player_join"));
        subscribeEvent.setPlayerQuit((boolean) subscribeEventConfig.get("player_quit"));
        subscribeEvent.setPlayerAdvancement((boolean) subscribeEventConfig.get("player_advancement"));
    }
}
