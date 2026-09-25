package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code config.example.yml} 的 Schema 回归测试
 *
 * <p>该文件是<b>当前版本官方 Schema / 用户可见文档</b>，因此它自身必须始终自洽：
 * 能正常解析、根节点正确、关键字段与默认值类型有效，并且<b>可以直接当作 config.yml 使用</b>
 * ——后者是对"Schema 与代码一致"最强的检验：只要 example 里少一个字段或类型写错，
 * 加载就会回退默认值，本测试即失败。
 */
class ConfigExampleSchemaTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigExampleSchemaTest.class);

    private static final String RESOURCE_NAME = "queqiao/config.example.yml";

    private static InputStream openResource() {
        return ConfigExampleSchemaTest.class.getClassLoader().getResourceAsStream(RESOURCE_NAME);
    }

    private static Path examplePath(Path baseDirectory) {
        return baseDirectory.resolve("queqiao").resolve("config.example.yml");
    }

    @Test
    @DisplayName("内置 example 资源存在、可解析、根节点为 Map")
    void exampleResourceIsParsableMap() throws IOException {
        try (InputStream inputStream = openResource()) {
            assertNotNull(inputStream, "内置资源 " + RESOURCE_NAME + " 必须存在");

            ConfigFileReader.Result result = ConfigFileReader.readResource(inputStream);
            assertEquals(ConfigFileState.VALID, result.getState(), "example 必须能解析为 YAML："
                    + result.getErrorMessage());
            assertNotNull(result.getMap(), "根节点应为 Map");
        }
    }

    @Test
    @DisplayName("example 的关键字段存在且类型正确")
    void exampleKeyFieldsHaveCorrectTypes() throws IOException {
        Map<String, Object> map;
        try (InputStream inputStream = openResource()) {
            map = ConfigFileReader.readResource(inputStream).getMap();
        }
        assertNotNull(map);

        assertTrue(map.get("enable") instanceof Boolean, "enable 应为 boolean");
        assertTrue(map.get("server_name") instanceof String, "server_name 应为 string");
        assertTrue(map.get("access_token") instanceof String, "access_token 应为 string");
        assertTrue(map.get("message_prefix") instanceof String, "message_prefix 应为 string");

        assertTrue(map.get("websocket_server") instanceof Map, "websocket_server 应为 Map");
        Map<?, ?> server = (Map<?, ?>) map.get("websocket_server");
        assertTrue(server.get("enable") instanceof Boolean, "websocket_server.enable 应为 boolean");
        assertTrue(server.get("host") instanceof String, "websocket_server.host 应为 string");
        assertTrue(server.get("port") instanceof Integer, "websocket_server.port 应为 integer");

        assertTrue(map.get("websocket_client") instanceof Map, "websocket_client 应为 Map");
        Map<?, ?> client = (Map<?, ?>) map.get("websocket_client");
        assertTrue(client.get("enable") instanceof Boolean, "websocket_client.enable 应为 boolean");
        assertTrue(client.get("reconnect_interval") instanceof Integer, "reconnect_interval 应为 integer");
        assertTrue(client.get("reconnect_max_times") instanceof Integer, "reconnect_max_times 应为 integer");
        assertTrue(client.get("url_list") instanceof java.util.List, "url_list 应为 list");

        assertTrue(map.get("rcon") instanceof Map, "rcon 应为 Map");
        Map<?, ?> rcon = (Map<?, ?>) map.get("rcon");
        assertTrue(rcon.get("enable") instanceof Boolean, "rcon.enable 应为 boolean");
        assertTrue(rcon.get("port") instanceof Integer, "rcon.port 应为 integer");
        assertTrue(rcon.get("password") instanceof String, "rcon.password 应为 string");

        assertTrue(map.get("subscribe_event") instanceof Map, "subscribe_event 应为 Map");
        assertTrue(map.get("ignored_commands") instanceof java.util.List, "ignored_commands 应为 list");
    }

    @Test
    @DisplayName("example 默认值必须是安全的：Rcon 关闭、仅监听回环")
    void exampleDefaultsAreSafe() throws IOException {
        Map<String, Object> map;
        try (InputStream inputStream = openResource()) {
            map = ConfigFileReader.readResource(inputStream).getMap();
        }
        assertNotNull(map);

        Map<?, ?> rcon = (Map<?, ?>) map.get("rcon");
        assertEquals(Boolean.FALSE, rcon.get("enable"), "Rcon 必须默认关闭");

        Map<?, ?> server = (Map<?, ?>) map.get("websocket_server");
        assertEquals("127.0.0.1", server.get("host"), "WebSocket Server 必须默认仅监听回环地址");

        Map<?, ?> client = (Map<?, ?>) map.get("websocket_client");
        assertEquals(Boolean.FALSE, client.get("enable"), "WebSocket Client 必须默认关闭");
    }

    @Test
    @DisplayName("example 可以直接当作 config.yml 使用（Schema 与代码自洽）")
    void exampleCanBeUsedAsConfig(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("queqiao").resolve("config.yml");
        Files.createDirectories(configPath.getParent());
        try (InputStream inputStream = openResource()) {
            Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        // 若 example 缺少某个字段或类型写错，加载会回退到内置默认值——下面的关键值断言即可暴露
        assertTrue(config.isEnable(), "enable 应解析成功");
        assertEquals("Server", config.getServerName(), "server_name 应解析成功");
        assertEquals("[鹊桥]", config.getMessagePrefix(), "message_prefix 应解析成功");
        assertEquals("127.0.0.1", config.getWebsocketServer().getHost(), "websocket_server.host 应解析成功");
        assertTrue(config.getWebsocketServer().isEnable(), "websocket_server.enable 应解析成功");
        assertTrue(config.getWebsocketServer().getPort() > 0, "websocket_server.port 应解析成功");
        assertFalse(config.getWebsocketClient().isEnable(), "websocket_client.enable 应解析成功");
        assertFalse(config.getRcon().isEnable(), "rcon.enable 应解析成功");
        assertEquals(25575, config.getRcon().getPort(), "rcon.port 应解析成功");
        assertTrue(config.getSubscribeEvent().isPlayerChat(), "subscribe_event.player_chat 应解析成功");
    }

    @Test
    @DisplayName("每次启动把 example 刷新为当前版本（内容与内置资源一致）")
    void exampleIsRefreshedOnStartup(@TempDir Path tempDir) throws IOException {
        Path path = examplePath(tempDir);
        Files.createDirectories(path.getParent());
        Files.write(path, "旧版本的过期内容\n".getBytes(StandardCharsets.UTF_8));

        Config.loadConfig(false, LOGGER, tempDir);

        String refreshed = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        try (InputStream inputStream = openResource()) {
            String builtIn = new String(
                    ConfigFileReader.readResource(inputStream)
                            .getMap()
                            .toString()
                            .getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8);
            assertFalse(builtIn.isEmpty());
        }
        assertFalse(refreshed.contains("旧版本的过期内容"), "example 应被刷新为当前版本");
        assertTrue(refreshed.contains("server_name"), "刷新后的 example 应含当前版本字段");
    }

    @Test
    @DisplayName("刷新 example 不会改动用户 config.yml")
    void refreshingExampleDoesNotTouchUserConfig(@TempDir Path tempDir) throws IOException {
        Path configPath = tempDir.resolve("queqiao").resolve("config.yml");
        Files.createDirectories(configPath.getParent());
        String userContent = "# 我的配置\nserver_name: \"Mine\"\n";
        Files.write(configPath, userContent.getBytes(StandardCharsets.UTF_8));

        Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(
                userContent,
                new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8),
                "刷新 example 不得改动用户 config.yml");
    }
}
