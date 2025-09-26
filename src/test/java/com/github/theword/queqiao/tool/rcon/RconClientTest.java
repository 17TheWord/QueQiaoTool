package com.github.theword.queqiao.tool.rcon;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RconClientTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    @org.junit.jupiter.api.Test
    public void testWrongPassword() throws IOException {
        RconClient rconClient = new RconClient(logger, 25575, "1234567890");
        rconClient.connect();
        Assertions.assertTrue(!rconClient.isConnected(), "Rcon 认证失败");
        String response = rconClient.sendCommand("list");
        logger.info("Rcon 响应: {}", response);
        Assertions.assertNotNull(response, "Rcon 响应为空");
        rconClient.stop();
        Assertions.assertFalse(rconClient.isConnected(), "Rcon 已关闭");
    }
}
