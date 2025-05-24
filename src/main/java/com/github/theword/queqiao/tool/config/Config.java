package com.github.theword.queqiao.tool.config;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

import static com.github.theword.queqiao.tool.utils.Tool.config;
import static com.github.theword.queqiao.tool.utils.Tool.logger;

/**
 * 配置项
 * 服务器初始化阶段请调用 {@link #loadConfig(boolean)}
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Config extends CommonConfig {
    /**
     * 是否启用插件/模组
     */
    private boolean enable = true;
    /**
     * 是否开启调试模式
     * <p>对详部分简写日志进行 logger.info 输出</p>
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
     * Contractor
     *
     * @param isModServer 是否为模组服务端
     */
    public Config(boolean isModServer) {
        String configFolder = isModServer ? "config" : "plugins";
        String serverType = isModServer ? "模组" : "插件";
        logger.info("当前服务端类型为：{}服", serverType);
        readConfigFile(configFolder, "config.yml");
    }

    /**
     * 加载配置文件
     * <p>服务端启动、初始化模组时调用</p>
     *
     * @param isModServer 是否为模组服务端
     * @return Config
     */
    public static Config loadConfig(boolean isModServer) {
        return new Config(isModServer);
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

        loadWebsocketServerConfig(configMap);
        loadWebsocketClientConfig(configMap);
        loadSubscribeEventConfig(configMap);
        config = this;
    }


    /**
     * 加载 WebSocket Server 配置项
     *
     * @param configMap WebSocket Server
     */
    @SuppressWarnings("unchecked")
    private void loadWebsocketServerConfig(Map<String, Object> configMap) {
        Map<String, Object> websocketServerConfig = (Map<String, Object>) configMap.get("websocket_server");
        websocketServer.setEnable((Boolean) websocketServerConfig.get("enable"));
        websocketServer.setHost((String) websocketServerConfig.get("host"));
        websocketServer.setPort((int) websocketServerConfig.get("port"));
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
