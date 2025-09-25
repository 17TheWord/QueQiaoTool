package com.github.theword.queqiao.tool.websocket;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WsClientTest {

  Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  void testUnicodeServerName() throws UnsupportedEncodingException {
    System.out.println(
        "URLEncoder: " + URLEncoder.encode("服务器", StandardCharsets.UTF_8.toString()));
  }

  @Test
  void testDecodeServerName() throws UnsupportedEncodingException {
    System.out.println(
        "URLDecoder:"
            + URLDecoder.decode("%E6%9C%8D%E5%8A%A1%E5%99%A8", StandardCharsets.UTF_8.toString()));
  }

  //    @Test
  void testClient() throws URISyntaxException {
    URI uri = new URI("ws://localhost:8080/minecraft/ws");
    WsClient wsClient = new WsClient(uri, logger, "Server", "", 5, 5, true);
    wsClient.connect();
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    String message =
        "{\n"
            + "    \"player\": {\n"
            + "        \"display_name\": \"17TheWord\",\n"
            + "        \"player_list_name\": \"17TheWord\",\n"
            + "        \"address\": \"/127.0.0.1:49271\",\n"
            + "        \"is_health_scaled\": false,\n"
            + "        \"health_scale\": 20.0,\n"
            + "        \"exp\": 0.0,\n"
            + "        \"total_exp\": 0,\n"
            + "        \"level\": 0,\n"
            + "        \"locale\": \"zh_cn\",\n"
            + "        \"ping\": 2,\n"
            + "        \"player_time\": 76758,\n"
            + "        \"is_player_time_relative\": true,\n"
            + "        \"player_time_offset\": 0,\n"
            + "        \"walk_speed\": 0.2,\n"
            + "        \"fly_speed\": 0.1,\n"
            + "        \"allow_flight\": true,\n"
            + "        \"is_sprinting\": false,\n"
            + "        \"is_sneaking\": false,\n"
            + "        \"is_flying\": false,\n"
            + "        \"is_op\": true,\n"
            + "        \"nickname\": \"17TheWord\",\n"
            + "        \"uuid\": \"aa96407b-9d4d-44f8-8d79-51d0dcfdedff\"\n"
            + "    },\n"
            + "    \"message\": \"easy\",\n"
            + "    \"message_id\": \"\",\n"
            + "    \"server_version\": \"1.20.1\",\n"
            + "    \"server_type\": \"spigot\",\n"
            + "    \"event_name\": \"AsyncPlayerChatEvent\",\n"
            + "    \"post_type\": \"message\",\n"
            + "    \"sub_type\": \"chat\",\n"
            + "    \"timestamp\": 1724652585,\n"
            + "    \"server_name\": \"Server\"\n"
            + "}\n";
    wsClient.send(message);
  }
}
