package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置加载与查漏补缺的行为测试
 *
 * <p>覆盖设计要求的核心保证：
 * <ul>
 *     <li><b>用户配置优先</b>，缺失字段用模板默认值补全（仅在内存）</li>
 *     <li><b>未知字段保留</b>，不删除、不参与运行时配置</li>
 *     <li><b>正常启动不重写 {@code config.yml}</b>——用户注释、顺序、格式逐字节保留</li>
 *     <li><b>YAML 解析失败时原文件绝不被覆盖</b>（INVALID ≠ EMPTY）</li>
 *     <li>类型错误与数值越界 → 内存使用默认值，且不修改用户文件</li>
 *     <li>{@code addons} 本身必须是 Map；其内部内容不校验、不删除、不告警</li>
 * </ul>
 */
class ConfigFileSynchronizationTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileSynchronizationTest.class);

    private static Path configPath(Path baseDirectory) {
        return baseDirectory.resolve("queqiao").resolve("config.yml");
    }

    private static void writeConfig(Path baseDirectory, String content) throws IOException {
        Path path = configPath(baseDirectory);
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String readConfig(Path baseDirectory) throws IOException {
        return new String(Files.readAllBytes(configPath(baseDirectory)), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // 基本行为
    // ------------------------------------------------------------------

    @Test
    @DisplayName("文件不存在时从模板生成，并同时释放 config.example.yml")
    void generatesConfigAndExampleFile(@TempDir Path tempDir) throws IOException {
        Config.loadConfig(false, LOGGER, tempDir);

        assertTrue(Files.exists(configPath(tempDir)), "应生成 config.yml");

        Path examplePath = tempDir.resolve("queqiao").resolve("config.example.yml");
        assertTrue(Files.exists(examplePath), "应同时释放当前版本的 config.example.yml");
        String example = new String(Files.readAllBytes(examplePath), StandardCharsets.UTF_8);
        assertTrue(example.contains("#"), "example 应保留模板注释（用户靠它了解当前版本配置）");
    }

    @Test
    @DisplayName("用户已写入的合法值优先于模板默认值")
    void userValuesWinOverTemplateDefaults(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "server_name: \"MyServer\"\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals("MyServer", config.getServerName());
    }

    @Test
    @DisplayName("缺失字段在内存中使用模板默认值")
    void missingFieldsFallBackToTemplateDefaults(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "server_name: \"MyServer\"\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertTrue(config.isEnable());
        assertEquals("[鹊桥]", config.getMessagePrefix());
        assertEquals(8080, config.getWebsocketServer().getPort(), "缺失的嵌套字段应使用模板默认值");
        assertEquals(25575, config.getRcon().getPort(), "整段缺失时应使用模板默认值");
    }

    // ------------------------------------------------------------------
    // 核心保证 1：正常启动不重写 config.yml
    // ------------------------------------------------------------------

    @Test
    @DisplayName("正常启动不重写 config.yml：用户注释、顺序、格式逐字节保留")
    void normalStartupDoesNotRewriteUserFile(@TempDir Path tempDir) throws IOException {
        String userContent = String.join(
                        "\n",
                        "# 我自己的注释",
                        "server_name: \"MyServer\"    # 行内注释",
                        "",
                        "# 下面是我提前写好的未知字段",
                        "my_custom_flag: true",
                        "")
                + "\n";
        writeConfig(tempDir, userContent);

        Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(userContent, readConfig(tempDir), "正常启动不得改写用户文件（注释/顺序/格式必须原样保留）");
    }

    @Test
    @DisplayName("未知字段被保留、不参与运行时配置")
    void unknownFieldsArePreservedAndIgnored(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "server_name: \"MyServer\"\nmy_custom_flag: true\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertTrue(readConfig(tempDir).contains("my_custom_flag"), "未知字段必须保留在文件中");
        assertEquals("MyServer", config.getServerName(), "已知字段照常生效");
    }

    // ------------------------------------------------------------------
    // 核心保证 2：类型 / 范围错误只影响内存
    // ------------------------------------------------------------------

    @Test
    @DisplayName("类型不匹配的字段在内存中回退默认值，且不修改用户文件")
    void typeMismatchFallsBackInMemoryOnly(@TempDir Path tempDir) throws IOException {
        String userContent = "websocket_server:\n  port: \"abc\"\n";
        writeConfig(tempDir, userContent);

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(8080, config.getWebsocketServer().getPort(), "类型错误的端口应回退默认值");
        assertEquals(userContent, readConfig(tempDir), "不得因类型错误而改写用户文件");
    }

    @Test
    @DisplayName("数值越界的端口在内存中回退默认值")
    void outOfRangePortFallsBackToDefault(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "websocket_server:\n  port: -1\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(8080, config.getWebsocketServer().getPort(), "越界端口应回退默认值");
    }

    @Test
    @DisplayName("重连间隔为 0 会回退默认值，避免立即重连风暴")
    void outOfRangeReconnectIntervalFallsBackToDefault(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "websocket_client:\n  reconnect_interval: 0\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(5, config.getWebsocketClient().getReconnectInterval(), "间隔 0 应回退为模板默认值");
    }

    // ------------------------------------------------------------------
    // 核心保证 3：INVALID 绝不覆盖原文件
    // ------------------------------------------------------------------

    @Test
    @DisplayName("YAML 解析失败：原文件保持不变，并使用安全默认配置")
    void invalidYamlNeverOverwritesUserFile(@TempDir Path tempDir) throws IOException {
        String brokenContent = String.join(
                        "\n",
                        "websocket_server:",
                        "   enable: true",
                        "    port: 8080",
                        "")
                + "\n";
        writeConfig(tempDir, brokenContent);

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(brokenContent, readConfig(tempDir), "解析失败时原文件绝不能被覆盖");
        assertEquals("Server", config.getServerName(), "应使用内置默认值");
        assertFalse(config.getRcon().isEnable(), "回退的默认值必须安全：Rcon 关闭");
        assertEquals("127.0.0.1", config.getWebsocketServer().getHost(), "回退的默认值必须安全：仅监听回环");
    }

    @Test
    @DisplayName("根节点不是 Map 时同样按 INVALID 处理，不覆盖原文件")
    void nonMapRootNeverOverwritesUserFile(@TempDir Path tempDir) throws IOException {
        String content = "- just\n- a\n- list\n";
        writeConfig(tempDir, content);

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals(content, readConfig(tempDir), "根节点类型错误时原文件绝不能被覆盖");
        assertEquals("Server", config.getServerName());
    }

    @Test
    @DisplayName("空文件与解析失败严格区分：空文件按默认值运行且不被写盘")
    void emptyFileIsNotTreatedAsInvalid(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals("Server", config.getServerName(), "空文件应使用默认值");
        assertEquals("", readConfig(tempDir), "空文件不被写盘（正常启动不写 config.yml）");
    }

    @Test
    @DisplayName("只有注释的文件同样按 EMPTY 处理")
    void commentOnlyFileIsTreatedAsEmpty(@TempDir Path tempDir) throws IOException {
        writeConfig(tempDir, "# 只有注释\n\n");

        Config config = Config.loadConfig(false, LOGGER, tempDir);

        assertEquals("Server", config.getServerName());
    }

    // ------------------------------------------------------------------
    // 核心保证 4：addons 命名空间
    // ------------------------------------------------------------------

    @Test
    @DisplayName("addons 内部字段不告警、不删除、不校验")
    void addonsNamespaceIsOpaqueToCore() {
        SyncReport report = ConfigSynchronizer.synchronize(
                parse("addons:\n  whatever: 1\n"), parse("addons:\n  queqiao_ai:\n    model: gpt\n    unknown_thing: 42\n"));

        assertTrue(report.getUnknown().isEmpty(), "addons 内部字段不应被报告为未知：" + report.getUnknown());
        assertTrue(report.getInvalid().isEmpty(), "addons 是 Map，不应被判为结构非法");
    }

    @Test
    @DisplayName("addons 本身不是 Map 时报告结构不合法，且原样保留")
    void addonsMustBeMap() {
        Map<String, Object> user = parse("addons: \"not-a-map\"\n");

        SyncReport report = ConfigSynchronizer.synchronize(parse("server_name: \"Server\"\n"), user);

        assertEquals(Collections.singletonList("addons"), report.getInvalid(), "addons 不是 Map 应被显式识别为结构不合法");
        assertEquals("not-a-map", user.get("addons"), "结构不合法的内容也应原样保留，不删除");
    }

    // ------------------------------------------------------------------
    // 组件：安全取值把"用户配置错误"与"程序缺陷"分开
    // ------------------------------------------------------------------

    @Test
    @DisplayName("安全取值：类型不符抛出 ConfigValidationException 并带字段路径")
    void safeAccessorsDistinguishUserErrorFromBug() {
        Map<String, Object> map = parse("enable: \"yes\"\n");

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> new ProbeConfig(LOGGER).readBoolean(map, "enable"));

        assertTrue(e.getMessage().contains("enable"), "错误信息应含字段路径：" + e.getMessage());
    }

    @Test
    @DisplayName("安全取值：YAML 留空（null）按空串处理，不判为非法")
    void safeAccessorsTolerateBlankString() {
        assertEquals("", new ProbeConfig(LOGGER).readString(parse("access_token:\n"), "access_token"));
    }

    @Test
    @DisplayName("内置默认值必须与模板默认值一致（守护回退路径）")
    void fieldDefaultsMatchTemplateDefaults(@TempDir Path tempDir) {
        Config fromFile = Config.loadConfig(false, LOGGER, tempDir);
        Config builtIn = Config.defaults(LOGGER);

        assertEquals(builtIn.getServerName(), fromFile.getServerName(), "server_name 默认值不一致");
        assertEquals(builtIn.isEnable(), fromFile.isEnable(), "enable 默认值不一致");
        assertEquals(builtIn.getMessagePrefix(), fromFile.getMessagePrefix(), "message_prefix 默认值不一致");
        assertEquals(
                builtIn.getWebsocketServer().getPort(),
                fromFile.getWebsocketServer().getPort(),
                "websocket_server.port 默认值不一致");
        assertEquals(
                builtIn.getWebsocketServer().getHost(),
                fromFile.getWebsocketServer().getHost(),
                "websocket_server.host 默认值不一致");
        assertEquals(
                builtIn.getWebsocketServer().isEnable(),
                fromFile.getWebsocketServer().isEnable(),
                "websocket_server.enable 默认值不一致");
        assertEquals(builtIn.getRcon().isEnable(), fromFile.getRcon().isEnable(), "rcon.enable 默认值不一致");
        assertEquals(builtIn.getRcon().getPort(), fromFile.getRcon().getPort(), "rcon.port 默认值不一致");
        assertEquals(
                builtIn.getWebsocketClient().isEnable(),
                fromFile.getWebsocketClient().isEnable(),
                "websocket_client.enable 默认值不一致");
        assertEquals(
                builtIn.getWebsocketClient().getReconnectInterval(),
                fromFile.getWebsocketClient().getReconnectInterval(),
                "reconnect_interval 默认值不一致");
        assertEquals(
                builtIn.getWebsocketClient().getReconnectMaxTimes(),
                fromFile.getWebsocketClient().getReconnectMaxTimes(),
                "reconnect_max_times 默认值不一致");
        assertEquals(
                builtIn.getSubscribeEvent().isPlayerChat(),
                fromFile.getSubscribeEvent().isPlayerChat(),
                "subscribe_event.player_chat 默认值不一致");

        assertNotNull(builtIn.getIgnoredCommands());
        assertTrue(
                builtIn.getIgnoredCommands().containsAll(Arrays.asList("l", "login", "register", "reg")),
                "内置默认值应包含默认忽略命令：" + builtIn.getIgnoredCommands());
    }

    // ------------------------------------------------------------------
    // 测试辅助
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) {
        return (Map<String, Object>) new Yaml().load(yaml);
    }

    /**
     * 暴露受保护的安全取值方法，用于直接测试
     */
    private static final class ProbeConfig extends CommonConfig {

        private ProbeConfig(Logger logger) {
            super(logger);
        }

        @Override
        protected void loadConfigValues(Map<String, Object> configMap) {
            // 测试用，无需实现
        }

        private boolean readBoolean(Map<String, Object> map, String key) {
            return requireBoolean(map, key);
        }

        private String readString(Map<String, Object> map, String key) {
            return requireString(map, key);
        }
    }
}
