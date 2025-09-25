package com.github.theword.queqiao.tool.websocket;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsServerTest {

  Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  void testUnicodeServerName() throws UnsupportedEncodingException {
    this.logger.info("URLEncoder: {}", URLEncoder.encode("服务器", StandardCharsets.UTF_8.toString()));
  }

  @Test
  void testDecodeServerName() throws UnsupportedEncodingException {
    this.logger.info(
        "URLDecoder:{}",
        URLDecoder.decode("%E6%9C%8D%E5%8A%A1%E5%99%A8", StandardCharsets.UTF_8.toString()));
  }

  @Test
  void testOnOpen() {
    InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 25565);
    WsServer wsServer = new WsServer(inetSocketAddress, logger, "Server", "", true);
    wsServer.start();
  }
}
