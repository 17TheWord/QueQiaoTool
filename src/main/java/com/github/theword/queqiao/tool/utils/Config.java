package com.github.theword.queqiao.tool.utils;


import com.github.theword.queqiao.tool.config.SubscribeEventConfig;
import com.github.theword.queqiao.tool.config.WebSocketClientConfig;
import com.github.theword.queqiao.tool.config.WebSocketServerConfig;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.github.theword.queqiao.tool.utils.Tool.logger;
import static com.github.theword.queqiao.tool.utils.Tool.config;

/**
 * 配置文件
 */
@Data
public class Config {
    /**
     * 是否启用组件
     */
    private boolean enable = true;
    /**
     * 是否开启调试模式
     */
    private boolean debug = false;

    /**
     * 服务器名
     */
    private String server_name = "Server";
    /**
     * 访问令牌
     */
    private String access_token = "";
    /**
     * 消息前缀
     */
    private String message_prefix = "鹊桥";

    /**
     * WebSocket Server 配置项
     */
    private WebSocketServerConfig websocket_server = new WebSocketServerConfig();
    /**
     * WebSocket Client 配置项
     */
    private WebSocketClientConfig websocket_client = new WebSocketClientConfig();
    /**
     * 订阅事件配置项
     */
    private SubscribeEventConfig subscribe_event = new SubscribeEventConfig();


    public Config(boolean isModServer) {
        logger.info("正在读取配置文件。");

        String configFolder = isModServer ? "config" : "plugins";
        String serverType = isModServer ? "模组" : "插件";

        Path configMapFilePath = Paths.get("./" + configFolder, BaseConstant.MODULE_NAME, "config.yml");

        logger.info("当前服务端类型为：{}服，配置文件路径为：{}。", serverType, configMapFilePath.toAbsolutePath());

        if (!Files.exists(configMapFilePath)) {
            logger.warn("配置文件不存在，即将生成默认配置文件。");
            try {
                InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("config.yml");
                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, configMapFilePath.toFile());
                logger.info("已生成默认配置文件。");
            } catch (IOException e) {
                logger.warn("生成配置文件失败。");
            }
        }

        try {
            Yaml yaml = new Yaml();
            Reader reader = Files.newBufferedReader(configMapFilePath);
            Map<String, Object> configMap = yaml.load(reader);
            loadConfigValues(configMap);
            logger.info("读取配置文件成功。");
            return;
        } catch (Exception e) {
            logger.warn("读取配置文件失败。");
            logger.warn(e.getMessage());
        }
        logger.warn("将直接使用默认配置项。");
    }

    public static Config loadConfig(boolean isModServer) {
        return new Config(isModServer);
    }

    private void loadConfigValues(Map<String, Object> configMap) {
        enable = (boolean) configMap.get("enable");
        debug = (boolean) configMap.get("debug");
        server_name = (String) configMap.get("server_name");
        access_token = (String) configMap.get("access_token");
        message_prefix = (String) configMap.get("message_prefix");

        loadWebsocketServerConfig(configMap);
        loadWebsocketClientConfig(configMap);
        loadSubscribeEventConfig(configMap);
        config = this;
    }


    private void loadWebsocketServerConfig(Map<String, Object> configMap) {
        Map<String, Object> websocketServerConfig = (Map<String, Object>) configMap.get("websocket_server");
        websocket_server.setEnable((Boolean) websocketServerConfig.get("enable"));
        websocket_server.setHost((String) websocketServerConfig.get("host"));
        websocket_server.setPort((int) websocketServerConfig.get("port"));
    }

    private void loadWebsocketClientConfig(Map<String, Object> configMap) {
        Map<String, Object> websocketClientConfig = (Map<String, Object>) configMap.get("websocket_client");
        websocket_client.setEnable((Boolean) websocketClientConfig.get("enable"));
        websocket_client.setReconnect_interval((int) websocketClientConfig.get("reconnect_interval"));
        websocket_client.setReconnect_max_times((int) websocketClientConfig.get("reconnect_max_times"));
        websocket_client.setUrl_list((List<String>) websocketClientConfig.get("url_list"));
    }

    private void loadSubscribeEventConfig(Map<String, Object> configMap) {
        Map<String, Object> subscribeEventConfig = (Map<String, Object>) configMap.get("subscribe_event");
        subscribe_event.setPlayer_chat((boolean) subscribeEventConfig.get("player_chat"));
        subscribe_event.setPlayer_command((boolean) subscribeEventConfig.get("player_command"));
        subscribe_event.setPlayer_death((boolean) subscribeEventConfig.get("player_death"));
        subscribe_event.setPlayer_join((boolean) subscribeEventConfig.get("player_join"));
        subscribe_event.setPlayer_quit((boolean) subscribeEventConfig.get("player_quit"));
    }
}
