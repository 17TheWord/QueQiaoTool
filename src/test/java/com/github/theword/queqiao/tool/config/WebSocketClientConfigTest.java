package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketClientConfigTest {

    @Test
    void testWebSocketClientConfig() {
        WebSocketClientConfig config = new WebSocketClientConfig();

        assertFalse(config.isEnable());
        assertEquals(5, config.getReconnectInterval());
        assertEquals(5, config.getReconnectMaxTimes());
        assertTrue(config.getUrlList().isEmpty());

        config.setEnable(true);
        config.setReconnectInterval(10);
        config.setReconnectMaxTimes(15);
        config.setUrlList(java.util.Arrays.asList("ws://example.com", "ws://example.org"));

        assertTrue(config.isEnable());
        assertEquals(10, config.getReconnectInterval());
        assertEquals(15, config.getReconnectMaxTimes());
        assertEquals(2, config.getUrlList().size());
        assertEquals("ws://example.com", config.getUrlList().get(0));
        assertEquals("ws://example.org", config.getUrlList().get(1));

        WebSocketClientConfig paramConfig = new WebSocketClientConfig(
                false, 20, 25, java.util.Arrays.asList("ws://example.net")
        );

        assertFalse(paramConfig.isEnable());
        assertEquals(20, paramConfig.getReconnectInterval());
        assertEquals(25, paramConfig.getReconnectMaxTimes());
        assertEquals(1, paramConfig.getUrlList().size());
        assertEquals("ws://example.net", paramConfig.getUrlList().get(0));
    }

}