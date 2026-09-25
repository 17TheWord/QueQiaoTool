package com.github.theword.queqiao.tool.config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

/**
 * 配置项 服务器初始化阶段请调用 {@link #loadConfig(boolean, Logger)} 方法加载配置文件
 */
public class Config extends CommonConfig {

    /**
     * 默认忽略的命令
     *
     * <p>注册与登录命令会被无条件忽略，避免玩家凭据进入事件流。
     */
    private static final Set<String> DEFAULT_IGNORED_COMMANDS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("l", "login", "register", "reg")));

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
     *
     * <p><b>字段初始值即"内置默认值"</b>：配置加载失败需要回退时，
     * 基类不会调用 {@code loadConfigValues}，而是直接保留字段初始值——
     * 因此这里的初始值必须与模板默认值一致（由
     * {@code ConfigFileSynchronizationTest#fieldDefaultsMatchTemplateDefaults} 守护）。
     *
     * <p>同时初始化为非 null 也修掉了"配置加载中途失败导致该字段为 null"的隐患
     * （此前 {@code Tool.isIgnoredCommand} 会因此抛 NPE）。
     */
    private Set<String> ignoredCommands = new HashSet<>(DEFAULT_IGNORED_COMMANDS);

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
        this(isModServer, logger, null);
    }

    /**
     * Contractor（可指定配置根目录）
     *
     * <p>显式指定 {@code baseDirectory} 后，配置读写不再依赖工作目录。
     * 生产可用于自定义数据目录；测试可用于 {@code @TempDir} 逐用例隔离。
     *
     * @param isModServer   是否为模组服务端
     * @param logger        日志实现
     * @param baseDirectory 配置根目录；为 null 时使用默认相对目录（{@code config} / {@code plugins}）
     */
    public Config(boolean isModServer, Logger logger, Path baseDirectory) {
        super(logger, baseDirectory);
        String configFolder = isModServer ? "config" : "plugins";
        String serverType = isModServer ? "模组" : "插件";
        logger.info("当前服务端类型为：{}服", serverType);
        readConfigFile(configFolder, "config.yml");
    }

    /**
     * 内部构造器：只初始化默认字段，不触碰文件系统
     *
     * @param logger 日志实现
     */
    private Config(Logger logger) {
        super(logger);
    }

    /**
     * 构造一份"全默认值"配置，<b>不读取也不写入任何文件</b>
     *
     * <p>用途：运行时空对象（尚未 {@code init} 或已 {@code shutdown}）需要一个可用的默认配置，
     * 使 {@code GlobalContext.getConfig()} 不返回 null，从根上消除调用方的空指针风险。
     *
     * <p>与 {@link #loadConfig(boolean, Logger)} 的关键区别：本方法不产生任何文件系统副作用。
     *
     * @param logger 日志实现
     * @return 默认配置，忽略命令列表已预置默认值
     */
    public static Config defaults(Logger logger) {
        // 字段初始值本身就是内置默认值，无需额外填充
        return new Config(logger);
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
     * 加载配置（可指定配置根目录）
     *
     * <p>测试使用 {@code @TempDir} 传入临时目录即可与工作目录完全隔离。
     *
     * @param isModServer   是否为模组服务端
     * @param logger        日志实现
     * @param baseDirectory 配置根目录；为 null 时使用默认相对目录
     * @return 已加载的配置
     */
    public static Config loadConfig(boolean isModServer, Logger logger, Path baseDirectory) {
        return new Config(isModServer, logger, baseDirectory);
    }

    /**
     * 加载配置文件
     *
     * @param configMap 配置文件内容
     */
    @Override
    protected void loadConfigValues(Map<String, Object> configMap) {
        enable = requireBoolean(configMap, "enable");
        debug = requireBoolean(configMap, "debug");
        serverName = requireString(configMap, "server_name");
        accessToken = requireString(configMap, "access_token");
        messagePrefix = requireString(configMap, "message_prefix");
        enableTranslation = requireBoolean(configMap, "enable_translation");

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
    private void loadIgnoredCommands(Map<String, Object> configMap) {
        ignoredCommands.clear();
        List<String> ignoredCommandList = optionalStringList(configMap, "ignored_commands");
        if (ignoredCommandList.isEmpty()) {
            super.getLogger().info("配置项 ignored_commands 为空，将只忽略默认的注册和登录命令");
        } else {
            ignoredCommands.addAll(ignoredCommandList);
            super.getLogger().info("已加载 {} 个忽略的命令", ignoredCommandList.size());
        }

        ignoredCommands.addAll(DEFAULT_IGNORED_COMMANDS);
    }

    /**
     * 加载 Rcon 客户端配置项
     *
     * @param configMap Rcon
     */
    private void loadRconConfig(Map<String, Object> configMap) {
        Map<String, Object> rconConfig = optionalMap(configMap, "rcon");
        if (rconConfig == null) {
            return;
        }
        rcon.setEnable(requireBoolean(rconConfig, "enable", "rcon.enable"));
        rcon.setPort(requireInt(rconConfig, "port", "rcon.port"));
        rcon.setPassword(requireString(rconConfig, "password", "rcon.password"));
    }

    /**
     * 加载 WebSocket Server 配置项
     *
     * @param configMap WebSocket Server
     */
    private void loadWebsocketServerConfig(Map<String, Object> configMap) {
        Map<String, Object> section = optionalMap(configMap, "websocket_server");
        if (section == null) {
            return;
        }
        websocketServer.setEnable(requireBoolean(section, "enable", "websocket_server.enable"));
        websocketServer.setHost(requireString(section, "host", "websocket_server.host"));
        websocketServer.setPort(requireInt(section, "port", "websocket_server.port"));
    }

    /**
     * 加载 WebSocket Client 配置项
     *
     * @param configMap WebSocket Client
     */
    private void loadWebsocketClientConfig(Map<String, Object> configMap) {
        Map<String, Object> section = optionalMap(configMap, "websocket_client");
        if (section == null) {
            return;
        }
        websocketClient.setEnable(requireBoolean(section, "enable", "websocket_client.enable"));
        websocketClient.setReconnectInterval(requireInt(section, "reconnect_interval", "websocket_client.reconnect_interval"));
        websocketClient.setReconnectMaxTimes(requireInt(section, "reconnect_max_times", "websocket_client.reconnect_max_times"));
        websocketClient.setUrlList(optionalStringList(section, "url_list"));
    }

    /**
     * 加载订阅事件配置项
     *
     * @param configMap SubscribeEvent
     */
    private void loadSubscribeEventConfig(Map<String, Object> configMap) {
        Map<String, Object> section = optionalMap(configMap, "subscribe_event");
        if (section == null) {
            return;
        }
        subscribeEvent.setPlayerChat(requireBoolean(section, "player_chat", "subscribe_event.player_chat"));
        subscribeEvent.setPlayerCommand(requireBoolean(section, "player_command", "subscribe_event.player_command"));
        subscribeEvent.setPlayerDeath(requireBoolean(section, "player_death", "subscribe_event.player_death"));
        subscribeEvent.setPlayerJoin(requireBoolean(section, "player_join", "subscribe_event.player_join"));
        subscribeEvent.setPlayerQuit(requireBoolean(section, "player_quit", "subscribe_event.player_quit"));
        subscribeEvent.setPlayerAdvancement(requireBoolean(section, "player_advancement", "subscribe_event.player_advancement"));
    }
}
