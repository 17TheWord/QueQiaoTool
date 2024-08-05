package com.github.theword.queqiao.utils;


import com.github.theword.queqiao.configs.SubscribeEventConfig;
import com.github.theword.queqiao.configs.WebSocketClientConfig;
import com.github.theword.queqiao.configs.WebSocketServerConfig;
import com.github.theword.queqiao.constant.BaseConstant;
import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.theword.queqiao.utils.Tool.logger;

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

    /**
     * 根据服务端类型加载配置文件
     *
     * @param isModServer 是否为模组服务端
     * @return Config
     */
    public static Config loadConfig(boolean isModServer) {
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
            Config config = yaml.loadAs(reader, Config.class);
            logger.info("读取配置文件成功。");
            return config;
        } catch (Exception e) {
            logger.warn("读取配置文件失败。");
        }
        logger.info("将直接使用默认配置项。");
        return new Config();
    }
}
