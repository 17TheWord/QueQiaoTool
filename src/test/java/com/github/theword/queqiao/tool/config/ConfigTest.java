package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Config} 默认值测试
 *
 * <p><b>测试隔离</b>：通过 {@code Config.loadConfig(isModServer, logger, baseDirectory)} 把配置读写
 * 指向 {@link TempDir}，不再依赖工作目录下的 {@code plugins/queqiao/config.yml}。
 */
class ConfigTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @DisplayName("首次加载生成模板配置，且默认值符合预期")
    void testDefaultConfig(@TempDir Path tempDir) {
        Config config = Config.loadConfig(false, logger, tempDir);
        logger.info("config: {}", config);

        assertEquals("Server", config.getServerName());
        assertTrue(config.isEnable());
        assertFalse(config.isDebug());
        assertEquals("", config.getAccessToken());
        assertEquals("[鹊桥]", config.getMessagePrefix());
        assertNotNull(config.getWebsocketServer());
        assertNotNull(config.getWebsocketClient());
        assertNotNull(config.getSubscribeEvent());
        assertNotNull(config.getRcon());
        // WebSocketServerConfig
        assertTrue(config.getWebsocketServer().isEnable());
        assertNotNull(config.getWebsocketServer().getHost());
        assertTrue(config.getWebsocketServer().getPort() > 0);
        // WebSocketClientConfig
        assertFalse(config.getWebsocketClient().isEnable());
        assertTrue(config.getWebsocketClient().getReconnectInterval() >= 0);
        assertTrue(config.getWebsocketClient().getReconnectMaxTimes() >= 0);
        assertNotNull(config.getWebsocketClient().getUrlList());
        // SubscribeEventConfig
        assertTrue(config.getSubscribeEvent().isPlayerChat());
        assertTrue(config.getSubscribeEvent().isPlayerCommand());
        assertTrue(config.getSubscribeEvent().isPlayerDeath());
        assertTrue(config.getSubscribeEvent().isPlayerJoin());
        assertTrue(config.getSubscribeEvent().isPlayerQuit());
        assertTrue(config.getSubscribeEvent().isPlayerAdvancement());
        // RconConfig
        assertFalse(config.getRcon().isEnable());
        assertEquals(25575, config.getRcon().getPort());
        assertEquals("", config.getRcon().getPassword());
    }

    /**
     * 隔离生效的证据：配置文件生成在传入的 baseDirectory 之下
     */
    @Test
    @DisplayName("配置读写落在指定的 baseDirectory 下（隔离生效）")
    void configIsReadAndWrittenUnderBaseDirectory(@TempDir Path tempDir) {
        Config.loadConfig(false, logger, tempDir);

        Path expected = tempDir.resolve("queqiao").resolve("config.yml");
        assertTrue(Files.exists(expected), "配置文件应生成在 baseDirectory 下：" + expected);
    }
}
