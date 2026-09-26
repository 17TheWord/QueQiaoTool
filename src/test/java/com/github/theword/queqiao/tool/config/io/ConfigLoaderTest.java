package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.schema.ConfigKey;
import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.schema.ConfigSectionNode;
import com.github.theword.queqiao.tool.config.codec.BooleanCodec;
import com.github.theword.queqiao.tool.config.codec.IntegerCodec;
import com.github.theword.queqiao.tool.config.codec.ListCodec;
import com.github.theword.queqiao.tool.config.codec.StringCodec;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.validation.ConfigValidators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigLoader} 测试
 *
 * <p>覆盖 Phase 4 补充约束 §24 的测试矩阵：文件状态、类型、配置状态、错误、
 * 未知字段、原子性、多 Runtime。
 */
class ConfigLoaderTest {

    private static final ConfigKey<Boolean> ENABLE = ConfigKey.builder("websocket_server.enable", BooleanCodec.INSTANCE)
            .defaultValue(true)
            .build();

    private static final ConfigKey<Integer> PORT = ConfigKey.builder("websocket_server.port", IntegerCodec.INSTANCE)
            .defaultValue(8080)
            .validator(ConfigValidators.range(1, 65535))
            .build();

    private static final ConfigKey<String> HOST = ConfigKey.builder("websocket_server.host", StringCodec.INSTANCE)
            .defaultValue("127.0.0.1")
            .build();

    private static final ConfigKey<List<String>> URL_LIST =
            ConfigKey.builder("websocket_client.url_list", ListCodec.INSTANCE)
                    .defaultValueSupplier(() -> new ArrayList<>(Arrays.asList("ws://a")))
                    .build();

    private ConfigRegistry registry;
    private Config runtime;
    private ConfigLoader loader;

    @BeforeEach
    void setUp() {
        registry = new ConfigRegistry();
        registry.register(ENABLE);
        registry.register(PORT);
        registry.register(HOST);
        registry.register(URL_LIST);
        registry.register(ConfigSectionNode.of("websocket_server").comment("WebSocket Server 配置"));
        runtime = new Config(registry);
        loader = new ConfigLoader(registry, runtime);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) {
        return (Map<String, Object>) new Yaml().load(yaml);
    }

    // ------------------------------------------------------------------
    // 文件状态（§3）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("MISSING：全部使用默认值，且不算错误")
    void missingUsesDefaults() {
        ConfigLoadResult result = loader.load(ConfigFileState.MISSING, null);

        assertEquals(ConfigFileState.MISSING, result.getState());
        assertEquals(8080, runtime.get(PORT));
        assertTrue(runtime.isDefault(PORT));
        assertFalse(result.hasUnknown());
    }

    @Test
    @DisplayName("EMPTY：与 MISSING 一致，全部使用默认值")
    void emptyUsesDefaults() {
        ConfigLoadResult result = loader.load(ConfigFileState.EMPTY, Collections.emptyMap());

        assertEquals(ConfigFileState.EMPTY, result.getState());
        assertEquals(8080, runtime.get(PORT));
        assertTrue(runtime.isDefault(PORT));
    }

    @Test
    @DisplayName("INVALID：抛错且不得改动运行时状态")
    void invalidDoesNotTouchRuntime() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 19132\n"));
        List<String> before = runtime.describeEntries();

        assertThrows(ConfigValidationException.class, () -> loader.load(ConfigFileState.INVALID, null));

        assertEquals(before, runtime.describeEntries(), "INVALID 必须保持原状态");
        assertEquals(19132, runtime.get(PORT));
    }

    // ------------------------------------------------------------------
    // 配置状态（§11 / §12）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("VALID：全部字段存在 → 全部为 USER")
    void allFieldsPresent() {
        loader.load(
                ConfigFileState.VALID,
                parse("websocket_server:\n  enable: false\n  port: 19132\n  host: \"0.0.0.0\"\n"
                        + "websocket_client:\n  url_list:\n    - \"ws://x\"\n"));

        assertFalse(runtime.get(ENABLE));
        assertEquals(19132, runtime.get(PORT));
        assertEquals("0.0.0.0", runtime.get(HOST));
        assertEquals(Collections.singletonList("ws://x"), runtime.get(URL_LIST));
        assertTrue(runtime.contains(PORT));
    }

    @Test
    @DisplayName("部分字段缺失：缺失者为 DEFAULT，存在者为 USER")
    void partialFields() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  enable: false\n"));

        assertTrue(runtime.contains(ENABLE));
        assertFalse(runtime.isDefault(ENABLE));
        assertEquals(8080, runtime.get(PORT));
        assertTrue(runtime.isDefault(PORT), "缺失字段来源必须是 DEFAULT");
        assertFalse(runtime.contains(PORT));
    }

    @Test
    @DisplayName("显式写了与默认值相同的值 → 仍为 USER")
    void explicitDefaultValueIsUser() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 8080\n"));

        assertEquals(8080, runtime.get(PORT));
        assertTrue(runtime.contains(PORT), "contains 必须为 true");
        assertFalse(runtime.isDefault(PORT), "isDefault 看来源，不看值");
    }

    // ------------------------------------------------------------------
    // 类型与校验（§9 / §21）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("类型错误：抛错且带完整路径与期望类型，状态不变")
    void wrongTypeIsRejected() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 19132\n"));
        List<String> before = runtime.describeEntries();

        ConfigValidationException e = assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: \"abc\"\n")));

        assertTrue(e.getMessage().contains("websocket_server.port"), e.getMessage());
        assertTrue(e.getMessage().contains("integer"), e.getMessage());
        assertEquals(before, runtime.describeEntries(), "类型错误必须保持原状态");
    }

    @Test
    @DisplayName("8080.0 不被接受为整数（不做宽松转换）")
    void doubleIsNotAcceptedAsInteger() {
        ConfigValidationException e = assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 8080.0\n")));

        assertTrue(e.getMessage().contains("websocket_server.port"), e.getMessage());
    }

    @Test
    @DisplayName("validator 失败：抛错且带范围，状态不变")
    void validatorFailureIsRejected() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 19132\n"));

        ConfigValidationException e = assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 99999\n")));

        assertTrue(e.getMessage().contains("65535"), e.getMessage());
        assertEquals(19132, runtime.get(PORT), "validator 失败必须保持原状态");
    }

    @Test
    @DisplayName("null 值：视为无效配置（非 Optional 配置项不接受 null）")
    void nullValueIsInvalid() {
        ConfigValidationException e = assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("websocket_server:\n  host: null\n")));

        assertTrue(e.getMessage().contains("websocket_server.host"), e.getMessage());
        assertTrue(e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    @DisplayName("区块被写成标量：明确报错")
    void sectionAsScalarIsRejected() {
        ConfigValidationException e = assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("websocket_server: \"not-a-map\"\n")));

        assertTrue(e.getMessage().contains("websocket_server"), e.getMessage());
    }

    @Test
    @DisplayName("根节点非 Mapping 由 ConfigFileReader 判为 INVALID；非 String 键被明确拒绝")
    void nonStringKeysAreRejected() {
        Map<Object, Object> raw = new LinkedHashMap<>();
        raw.put(123, "x");

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> new ConfigDocument(raw));

        assertTrue(e.getMessage().contains("123"), e.getMessage());
        assertTrue(e.getMessage().contains("字符串"), e.getMessage());
    }

    // ------------------------------------------------------------------
    // 未知字段（§14 / §15 / §16 / §17 / §20）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("未知核心字段：记录但阻止不了提交，且不进入 Runtime")
    void unknownCoreFieldIsRecorded() {
        ConfigLoadResult result = loader.load(
                ConfigFileState.VALID, parse("websocket_server:\n  enable: false\n  hello: world\n"));

        assertEquals(Collections.singletonList("websocket_server.hello"), result.getUnknownCorePaths());
        assertTrue(result.getUnknownAddonPaths().isEmpty());
        assertFalse(runtime.get(ENABLE), "已知字段应正常生效");
        assertTrue(result.getDocument().has("websocket_server.hello"), "未知字段必须保留在文档层");
    }

    @Test
    @DisplayName("未知顶层字段同样只记录")
    void unknownTopLevelFieldIsRecorded() {
        ConfigLoadResult result = loader.load(ConfigFileState.VALID, parse("my_custom_flag: true\n"));

        assertEquals(Collections.singletonList("my_custom_flag"), result.getUnknownCorePaths());
    }

    @Test
    @DisplayName("addons 下的未知字段：单独收集、保留、不报成核心未知")
    void unknownAddonsFieldIsPreservedSeparately() {
        ConfigLoadResult result = loader.load(
                ConfigFileState.VALID, parse("addons:\n  llm:\n    model: gpt\n    mystery: 42\n"));

        assertTrue(result.getUnknownCorePaths().isEmpty(), "不应报成核心未知：" + result.getUnknownCorePaths());
        assertEquals(
                Arrays.asList("addons.llm.model", "addons.llm.mystery"),
                result.getUnknownAddonPaths());
        assertTrue(result.getDocument().has("addons.llm.mystery"), "addons 内容必须保留");
    }

    @Test
    @DisplayName("addons 不是 Mapping：仍然非法（不因是扩展区而放弃结构校验）")
    void addonsMustBeMapping() {
        assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("addons: hello\n")));
        assertThrows(
                ConfigValidationException.class,
                () -> loader.load(ConfigFileState.VALID, parse("addons:\n  - hello\n")));
    }

    @Test
    @DisplayName("路径必须完整段匹配：port 不会误匹配 port_backup")
    void pathMatchingIsExact() {
        ConfigLoadResult result = loader.load(
                ConfigFileState.VALID, parse("websocket_server:\n  port_backup: 19132\n"));

        assertEquals(Collections.singletonList("websocket_server.port_backup"), result.getUnknownCorePaths());
        assertEquals(8080, runtime.get(PORT), "port 未被赋值，应保持默认值");
        assertFalse(runtime.contains(PORT));
    }

    // ------------------------------------------------------------------
    // 原子性与多 Runtime（§19 / §24）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("原子性：一个字段合法、另一个非法 → 整次 load 失败，状态完全不变")
    void loadIsAtomicAcrossFields() {
        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 19132\n  host: \"1.2.3.4\"\n"));
        List<String> before = runtime.describeEntries();

        assertThrows(
                ConfigValidationException.class,
                () -> loader.load(
                        ConfigFileState.VALID,
                        parse("websocket_server:\n  port: 19133\n  host: 12345\n")));

        assertEquals(before, runtime.describeEntries(), "整次 load 必须原子失败");
        assertEquals(19132, runtime.get(PORT));
        assertEquals("1.2.3.4", runtime.get(HOST));
    }

    @Test
    @DisplayName("两个 Runtime 加载互不影响")
    void twoRuntimesAreIndependent() {
        Config other = new Config(registry);
        ConfigLoader otherLoader = new ConfigLoader(registry, other);

        loader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 19132\n"));
        otherLoader.load(ConfigFileState.VALID, parse("websocket_server:\n  port: 25565\n"));

        assertEquals(19132, runtime.get(PORT));
        assertEquals(25565, other.get(PORT));
    }
}
