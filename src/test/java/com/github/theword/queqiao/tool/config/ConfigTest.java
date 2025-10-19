package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testDefaultConfig() {
        Config config = Config.loadConfig(false, logger);
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
}
