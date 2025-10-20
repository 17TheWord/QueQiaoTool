package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RconConfigTest {
    @Test
    void testRconConfigSetAndGet() {
        RconConfig config = new RconConfig();
        assertFalse(config.isEnable(), "默认应为false");
        assertEquals(25575, config.getPort(), "默认端口应为25575");
        assertEquals("", config.getPassword(), "默认密码应为空字符串");
        config.setEnable(true);
        config.setPort(12345);
        config.setPassword("abc123");
        assertTrue(config.isEnable(), "设置为true后应为true");
        assertEquals(12345, config.getPort());
        assertEquals("abc123", config.getPassword());
    }
}