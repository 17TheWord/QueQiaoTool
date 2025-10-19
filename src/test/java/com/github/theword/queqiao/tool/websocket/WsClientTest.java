package com.github.theword.queqiao.tool.websocket;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WsClientTest {

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

    //    @Test
    void testClient() throws URISyntaxException {
        URI uri = new URI("ws://localhost:8080/minecraft/ws");
        WsClient wsClient = new WsClient(uri, logger, gson, "Server", "", 5, 5, true);
        wsClient.connect();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String message = "{ \"player\": { \"display_name\": \"17TheWord\", \"player_list_name\": \"17TheWord\", \"address\": \"/127.0.0.1:49271\", \"is_health_scaled\": false, \"health_scale\": 20.0, \"exp\": 0.0, \"total_exp\": 0, \"level\": 0, \"locale\": \"zh_cn\", \"ping\": 2, \"player_time\": 76758, \"is_player_time_relative\": true, \"player_time_offset\": 0, \"walk_speed\": 0.2, \"fly_speed\": 0.1, \"allow_flight\": true, \"is_sprinting\": false, \"is_sneaking\": false, \"is_flying\": false, \"is_op\": true, \"nickname\": \"17TheWord\", \"uuid\": \"aa96407b-9d4d-44f8-8d79-51d0dcfdedff\" }, \"message\": \"easy\", \"message_id\": \"\", \"server_version\": \"1.20.1\", \"server_type\": \"spigot\", \"event_name\": \"AsyncPlayerChatEvent\", \"post_type\": \"message\", \"sub_type\": \"chat\", \"timestamp\": 1724652585, \"server_name\": \"Server\" }";
        wsClient.send(message);
    }
}
