package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketServerConfigTest {

    @Test
    void testWebSocketServerConfig() {
        WebSocketServerConfig config = new WebSocketServerConfig();
        assertTrue(config.isEnable());
        assertEquals("127.0.0.1", config.getHost());
        assertEquals(8080, config.getPort());

        config.setEnable(false);
        config.setHost("192.168.1.1");
        config.setPort(9000);
        assertFalse(config.isEnable());
        assertEquals("192.168.1.1", config.getHost());
        assertEquals(9000, config.getPort());

        WebSocketServerConfig config2 = new WebSocketServerConfig(true, "10.0.0.1", 1234);
        assertTrue(config2.isEnable());
        assertEquals("10.0.0.1", config2.getHost());
        assertEquals(1234, config2.getPort());
    }
}
