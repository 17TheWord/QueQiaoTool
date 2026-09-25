package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.config.codec.BooleanCodec;
import com.github.theword.queqiao.tool.config.codec.IntegerCodec;
import com.github.theword.queqiao.tool.config.codec.ListCodec;
import com.github.theword.queqiao.tool.config.codec.StringCodec;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.validation.ConfigValidators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Config} 行为测试
 *
 * <p>覆盖 Phase 3 补充约束 §17 的全部条目：基本读写、类型/校验、可变默认值、
 * User vs Default、load 原子性、Registry 归属、Runtime 隔离。
 */
class ConfigTest {

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

    private static final ConfigKey<List<String>> URL_LIST = ConfigKey.builder("websocket_client.url_list", ListCodec.INSTANCE)
            .defaultValueSupplier(() -> new ArrayList<>(Arrays.asList("ws://a")))
            .build();

    private static ConfigRegistry newRegistry() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(ENABLE);
        registry.register(PORT);
        registry.register(HOST);
        registry.register(URL_LIST);
        return registry;
    }

    private static Config newRuntime() {
        return new Config(newRegistry());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) {
        return (Map<String, Object>) new Yaml().load(yaml);
    }

    // ------------------------------------------------------------------
    // 基本
    // ------------------------------------------------------------------

    @Test
    @DisplayName("get：未加载时返回默认值，不是 null")
    void getReturnsDefaultWhenEmpty() {
        Config runtime = newRuntime();

        assertEquals(Boolean.TRUE, runtime.get(ENABLE));
        assertEquals(8080, runtime.get(PORT));
        assertEquals("127.0.0.1", runtime.get(HOST));
    }

    @Test
    @DisplayName("set / get / reset：写入、读回、恢复默认")
    void setGetReset() {
        Config runtime = newRuntime();

        runtime.set(PORT, 19132);
        assertEquals(19132, runtime.get(PORT));
        assertTrue(runtime.contains(PORT));
        assertFalse(runtime.isDefault(PORT));

        runtime.reset(PORT);
        assertEquals(8080, runtime.get(PORT));
        assertFalse(runtime.contains(PORT));
        assertTrue(runtime.isDefault(PORT));
    }

    @Test
    @DisplayName("contains / isDefault：缺失字段的语义")
    void containsAndIsDefaultForMissingField() {
        Config runtime = newRuntime();

        assertEquals(8080, runtime.get(PORT), "缺失时 get 返回默认值");
        assertFalse(runtime.contains(PORT), "但 contains 必须为 false");
        assertTrue(runtime.isDefault(PORT));
    }

    // ------------------------------------------------------------------
    // 类型 / 校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("set：类型不符抛异常")
    void setWithWrongTypeIsRejected() {
        Config runtime = newRuntime();

        @SuppressWarnings({"unchecked", "rawtypes"})
        ConfigKey rawPort = PORT;

        assertThrows(ConfigValidationException.class, () -> runtime.set(rawPort, 8080.0));
        assertThrows(ConfigValidationException.class, () -> runtime.set(rawPort, "8080"));
    }

    @Test
    @DisplayName("set：validator 不通过抛异常，且带路径与范围")
    void setWithInvalidValueIsRejected() {
        Config runtime = newRuntime();

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> runtime.set(PORT, 99999));
        assertTrue(e.getMessage().contains("websocket_server.port"), e.getMessage());
        assertTrue(e.getMessage().contains("65535"), e.getMessage());
    }

    @Test
    @DisplayName("set 失败不污染旧值（异常安全）")
    void failedSetKeepsOldValue() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  port: 19132\n"));

        assertThrows(ConfigValidationException.class, () -> runtime.set(PORT, 99999));

        assertEquals(19132, runtime.get(PORT), "set 失败后必须保持旧值");
        assertTrue(runtime.contains(PORT), "来源也不应被改动");
    }

    // ------------------------------------------------------------------
    // 可变值：不暴露内部引用
    // ------------------------------------------------------------------

    @Test
    @DisplayName("get 返回副本：修改返回值不影响内部状态")
    void getDoesNotExposeInternalReference() {
        Config runtime = newRuntime();
        runtime.set(URL_LIST, new ArrayList<>(Arrays.asList("ws://a")));

        List<String> got = runtime.get(URL_LIST);
        got.add("ws://injected");

        assertEquals(1, runtime.get(URL_LIST).size(), "修改 get() 的返回值不得改变内部状态");
    }

    @Test
    @DisplayName("snapshot 返回副本：修改快照值不影响 Runtime")
    void snapshotDoesNotExposeInternalReference() {
        Config runtime = newRuntime();
        runtime.set(URL_LIST, new ArrayList<>(Arrays.asList("ws://a")));

        ConfigSnapshot snapshot = runtime.snapshot();
        List<String> fromSnapshot = snapshot.valueOf(URL_LIST);
        fromSnapshot.add("ws://injected");

        assertEquals(Arrays.asList("ws://a"), runtime.get(URL_LIST));
        assertEquals(Arrays.asList("ws://a"), snapshot.valueOf(URL_LIST));
    }

    @Test
    @DisplayName("两个 Runtime 不共享同一个可变默认值对象")
    void mutableDefaultIsNotSharedBetweenRuntimes() {
        ConfigRegistry registry = newRegistry();
        Config a = new Config(registry);
        Config b = new Config(registry);

        List<String> fromA = a.get(URL_LIST);
        List<String> fromB = b.get(URL_LIST);
        assertNotSame(fromA, fromB);

        fromA.add("ws://x");
        assertEquals(1, b.get(URL_LIST).size(), "修改一个 Runtime 的默认值不应影响另一个");
    }

    @Test
    @DisplayName("reset 每次返回独立的可变默认值")
    void resetReturnsFreshMutableDefault() {
        Config runtime = newRuntime();

        runtime.reset(URL_LIST);
        List<String> first = runtime.get(URL_LIST);
        first.add("ws://x");

        runtime.reset(URL_LIST);
        assertEquals(1, runtime.get(URL_LIST).size(), "下一次 reset 应得到独立的默认值");
        assertEquals(Arrays.asList("ws://a"), URL_LIST.defaultValue(), "ConfigKey 的默认值不得被改动");
    }

    // ------------------------------------------------------------------
    // User vs Default：值相同也算 USER
    // ------------------------------------------------------------------

    @Test
    @DisplayName("用户显式写了与默认值相同的值，仍属 USER")
    void explicitDefaultValueIsStillUser() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  port: 8080\n"));

        assertEquals(8080, runtime.get(PORT));
        assertTrue(runtime.contains(PORT), "用户显式写了 → contains 必须为 true");
        assertFalse(runtime.isDefault(PORT), "isDefault 看来源，不看值是否等于默认值");
    }

    // ------------------------------------------------------------------
    // load：原子性 / 缺失即默认 / 不改 ConfigKey
    // ------------------------------------------------------------------

    @Test
    @DisplayName("load：缺失字段记为默认值，不算错误")
    void loadTreatsMissingAsDefault() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  enable: false\n"));

        assertFalse(runtime.get(ENABLE));
        assertTrue(runtime.contains(ENABLE));
        assertEquals(8080, runtime.get(PORT), "缺失字段用默认值");
        assertTrue(runtime.isDefault(PORT));
    }

    @Test
    @DisplayName("load 原子性：任何一项失败都保持原状态（不能半新半旧）")
    void loadIsAtomic() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  enable: false\n  port: 19132\n"));
        List<String> before = runtime.describeEntries();

        // 文档中 port 类型错误 → 整次 load 必须失败
        assertThrows(
                ConfigValidationException.class,
                () -> runtime.load(parse("websocket_server:\n  enable: true\n  port: \"abc\"\n")));

        assertEquals(before, runtime.describeEntries(), "load 失败后必须完全保持原状态");
        assertEquals(19132, runtime.get(PORT));
        assertFalse(runtime.get(ENABLE));
    }

    @Test
    @DisplayName("load 原子性：validator 失败同样保持原状态")
    void loadAtomicOnValidatorFailure() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  port: 19132\n"));

        assertThrows(
                ConfigValidationException.class,
                () -> runtime.load(parse("websocket_server:\n  port: 99999\n")));

        assertEquals(19132, runtime.get(PORT));
    }

    @Test
    @DisplayName("load 原子性：列表中的 null 元素失败时保持旧状态")
    void loadAtomicOnNullListElement() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_client:\n  url_list:\n    - ws://old\n"));

        assertThrows(
                ConfigValidationException.class,
                () -> runtime.load(parse("websocket_client:\n  url_list:\n    - ws://new\n    - null\n")));

        assertEquals(Arrays.asList("ws://old"), runtime.get(URL_LIST));
    }

    @Test
    @DisplayName("load 不修改 ConfigKey（Schema 不可变）")
    void loadDoesNotModifyConfigKey() {
        Config runtime = newRuntime();
        runtime.load(parse("websocket_server:\n  port: 19132\n"));

        assertEquals(8080, PORT.defaultValue(), "ConfigKey 是不可变 Schema，不得被 load 修改");
        assertEquals(19132, runtime.get(PORT));
    }

    // ------------------------------------------------------------------
    // Registry 归属与 Runtime 隔离
    // ------------------------------------------------------------------

    @Test
    @DisplayName("外来 Key 被拒绝：path 相同但对象不同")
    void foreignKeyIsRejected() {
        Config runtime = newRuntime();
        ConfigKey<Integer> foreign =
                ConfigKey.builder("websocket_server.port", IntegerCodec.INSTANCE).defaultValue(1).build();

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> runtime.get(foreign));
        assertTrue(e.getMessage().contains("websocket_server.port"), e.getMessage());

        assertThrows(ConfigValidationException.class, () -> runtime.set(foreign, 1));
        assertThrows(ConfigValidationException.class, () -> runtime.contains(foreign));
        assertThrows(ConfigValidationException.class, () -> runtime.reset(foreign));
    }

    @Test
    @DisplayName("两个 Runtime 相互隔离（共享同一 Registry 与 ConfigKey）")
    void runtimesAreIsolated() {
        ConfigRegistry registry = newRegistry();
        Config a = new Config(registry);
        Config b = new Config(registry);

        a.load(parse("websocket_server:\n  port: 19132\n"));

        assertEquals(19132, a.get(PORT));
        assertEquals(8080, b.get(PORT), "另一个 Runtime 不应受影响");
        assertFalse(b.contains(PORT));

        a.set(URL_LIST, new ArrayList<>(Arrays.asList("ws://a-only")));
        assertEquals(1, b.get(URL_LIST).size());
        assertEquals("ws://a", b.get(URL_LIST).get(0));
    }
}
