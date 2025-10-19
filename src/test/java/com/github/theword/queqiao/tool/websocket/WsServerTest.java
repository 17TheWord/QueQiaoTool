package com.github.theword.queqiao.tool.websocket;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WsServerTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Gson gson = new Gson();

    @Test
    void testUnicodeServerName() throws UnsupportedEncodingException {
        String encode = URLEncoder.encode("服务器", StandardCharsets.UTF_8.toString());
        this.logger.info("URLEncoder: {}", encode);
        assertEquals("%E6%9C%8D%E5%8A%A1%E5%99%A8", encode);
    }

    @Test
    void testDecodeServerName() throws UnsupportedEncodingException {
        String decode = URLDecoder.decode("%E6%9C%8D%E5%8A%A1%E5%99%A8", StandardCharsets.UTF_8.toString());
        this.logger.info("URLDecoder:{}", decode);
        assertEquals("服务器", decode);
    }

    @Test
    void testOnOpen() {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 25565);
        WsServer wsServer = new WsServer(inetSocketAddress, logger, gson, "Server", "", true);
        wsServer.start();
        assertNotNull(wsServer);
    }
}
