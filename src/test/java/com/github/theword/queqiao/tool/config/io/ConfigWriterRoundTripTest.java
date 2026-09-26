package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.schema.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.codec.ListCodec;
import com.github.theword.queqiao.tool.config.codec.StringCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 金丝雀闭环测试：<b>write → read → load</b>
 *
 * <p>这是 Phase 5 最重要的测试——只要写出的 YAML 有任何一处不能被自己读回，
 * 这里就会失败。尤其覆盖"看起来像其它类型的字符串"这类 YAML 序列化最容易出错的地方。
 */
class ConfigWriterRoundTripTest {

    private final ConfigWriter writer = new ConfigWriter();

    private static Map<String, Object> readYaml(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) {
        return (Map<String, Object>) new Yaml().load(yaml);
    }

    // ------------------------------------------------------------------
    // 核心闭环
    // ------------------------------------------------------------------

    @Test
    @DisplayName("闭环：写入 → 读回 → 加载，逐 Key 取值一致")
    void roundTripPreservesAllValues(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config original = new Config(registry);

        // 若干非默认值 + 代表性列表
        original.set(ConfigKeys.SERVER_NAME, "MyServer");
        original.set(ConfigKeys.DEBUG, true);
        original.set(ConfigKeys.WebSocket.PORT, 19132);
        original.set(ConfigKeys.WebSocketClient.URL_LIST, Arrays.asList("ws://a", "ws://b"));
        original.set(ConfigKeys.IGNORED_COMMANDS, Arrays.asList("say", "help"));
        original.set(ConfigKeys.Rcon.PASSWORD, "s3cret");

        Path target = tempDir.resolve("config.yml");
        writer.write(ConfigWriteSnapshot.of(registry, original, ConfigDocument.empty()), target, null);

        Config reloaded = new Config(registry);
        new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

        for (ConfigKey<?> key : ConfigKeys.all()) {
            assertEquals(original.get(key), reloaded.get(key), "字段回读不一致：" + key.getPath());
        }
    }

    @Test
    @DisplayName("闭环：USER 来源保持 USER")
    void roundTripPreservesUserSource(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config original = new Config(registry);
        original.set(ConfigKeys.SERVER_NAME, "MyServer");

        Path target = tempDir.resolve("config.yml");
        writer.write(ConfigWriteSnapshot.of(registry, original, ConfigDocument.empty()), target, null);

        Config reloaded = new Config(registry);
        new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

        assertTrue(reloaded.contains(ConfigKeys.SERVER_NAME), "USER 来源必须保持 USER");
        assertFalse(reloaded.isDefault(ConfigKeys.SERVER_NAME));

        // §31：默认值一旦被写入文件，再读回自然是 USER——这是正确行为
        assertTrue(reloaded.contains(ConfigKeys.DEBUG), "被写入文件的默认值读回后应为 USER");
        assertFalse(reloaded.isDefault(ConfigKeys.DEBUG));
    }

    // ------------------------------------------------------------------
    // 特殊字符串（§29）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("闭环：特殊字符串原样回读（含 true / 123 / null 这类歧义值）")
    void specialStringsRoundTrip(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKey<String> text = ConfigKey.builder("text", StringCodec.INSTANCE).defaultValue("").build();
        registry.register(text);
        Config runtime = new Config(registry);
        Path target = tempDir.resolve("t.yml");

        List<String> specials = Arrays.asList(
                "simple", "hello world", "hello: world", "#hello", "-hello", "[hello]", "{hello}",
                "true", "false", "123", "1.5", "null", "~", "", "ws://127.0.0.1:8080/minecraft/ws",
                "@everyone", "*star", "&anchor", "|pipe", ">gt", "%percent", "`tick");

        for (String value : specials) {
            runtime.set(text, value);
            writer.write(ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty()), target, null);

            Config reloaded = new Config(registry);
            new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

            assertEquals(value, reloaded.get(text), "字符串必须原样回读：[" + value + "]");
        }
    }

    @Test
    @DisplayName("闭环：列表（空 / 单项 / 多项 / 特殊字符串）完全等价")
    void listsRoundTrip(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKey<List<String>> list = ConfigKey.builder("list", ListCodec.INSTANCE)
                .defaultValueSupplier(ArrayList::new)
                .build();
        registry.register(list);
        Config runtime = new Config(registry);
        Path target = tempDir.resolve("t.yml");

        List<List<String>> cases = Arrays.asList(
                Collections.<String>emptyList(),
                Collections.singletonList("only"),
                Arrays.asList("foo", "hello: world", "#test", "123"),
                Arrays.asList("true", "null", "~", ""));

        for (List<String> value : cases) {
            runtime.set(list, value);
            writer.write(ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty()), target, null);

            Config reloaded = new Config(registry);
            new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

            assertEquals(value, reloaded.get(list), "列表必须完全等价：" + value);
        }
    }

    // ------------------------------------------------------------------
    // 未知字段闭环（§21 / §22）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("闭环：addons 下未加载 Addon 的配置必须存活（load → save → load）")
    void unknownAddonsSurviveRoundTrip(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        ConfigLoader loader = new ConfigLoader(registry, runtime);
        Path target = tempDir.resolve("config.yml");

        // 第一次 load：文档里含未加载 Addon 的配置
        ConfigLoadResult first = loader.load(
                ConfigFileState.VALID,
                parse("addons:\n  unloaded:\n    foo: bar\n    num: 123\n"
                        + "server_name: \"MyServer\"\n"));

        assertTrue(first.getUnknownAddonPaths().contains("addons.unloaded.foo"), first.getUnknownAddonPaths().toString());

        // save（必须保留未知字段）
        writer.write(ConfigWriteSnapshot.of(registry, runtime, first.getDocument()), target, null);

        // 再 load
        Config reloaded = new Config(registry);
        ConfigLoadResult second = new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

        assertEquals("MyServer", reloaded.get(ConfigKeys.SERVER_NAME), "已知字段应正常");
        assertTrue(second.getDocument().has("addons.unloaded.foo"), "addons 未知字段必须存活");
        assertEquals("bar", second.getDocument().get("addons.unloaded.foo"));
        assertEquals(123, second.getDocument().get("addons.unloaded.num"));
    }

    @Test
    @DisplayName("闭环：核心未知字段在 save 时也必须存活（save ≠ sync）")
    void unknownCoreFieldsSurviveSave(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        ConfigLoader loader = new ConfigLoader(registry, runtime);
        Path target = tempDir.resolve("config.yml");

        ConfigLoadResult first = loader.load(
                ConfigFileState.VALID,
                parse("server_name: \"MyServer\"\nmy_custom_flag: true\n"));

        assertEquals(
                Collections.singletonList("my_custom_flag"), first.getUnknownCorePaths());

        writer.write(ConfigWriteSnapshot.of(registry, runtime, first.getDocument()), target, null);

        Config reloaded = new Config(registry);
        ConfigLoadResult second = new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

        assertEquals(
                Collections.singletonList("my_custom_flag"),
                second.getUnknownCorePaths(),
                "save 不得隐式执行 sync：未知核心字段必须保留");
    }

    @Test
    @DisplayName("闭环：未知嵌套 YAML 值及特殊键名保存后保持等价")
    void unknownNestedYamlValuesAndKeysSurviveRoundTrip(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        ConfigLoadResult first = new ConfigLoader(registry, runtime).load(
                ConfigFileState.VALID,
                parse("addons:\n"
                        + "  future:\n"
                        + "    \"odd:key\":\n"
                        + "      - name: alpha\n"
                        + "        flags: [true, false]\n"
                        + "      - name: beta\n"
                        + "    nested:\n"
                        + "      - - a\n"
                        + "        - b\n"));

        Path target = tempDir.resolve("config.yml");
        writer.write(ConfigWriteSnapshot.of(registry, runtime, first.getDocument()), target, null);

        ConfigDocument written = new ConfigDocument(readYaml(target));
        assertEquals(first.getDocument().get("addons"), written.get("addons"));
    }

    // ------------------------------------------------------------------
    // 首次生成（§27）
    // ------------------------------------------------------------------

    @Test
    @DisplayName("首次生成：生成的文档必须是合法 YAML 且能被 Loader 正常加载")
    void firstGenerationIsLoadable(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        Path target = tempDir.resolve("config.yml");

        writer.write(ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty()), target, null);

        String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        assertTrue(content.contains("#"), "首次生成应带注释");
        assertTrue(content.contains("websocket_server:"), "首次生成应含全部区块");

        Config reloaded = new Config(registry);
        ConfigLoadResult result = new ConfigLoader(registry, reloaded).load(ConfigFileState.VALID, readYaml(target));

        assertFalse(result.hasUnknown(), "首次生成不应产生未知字段：" + result.getUnknownCorePaths());
        for (ConfigKey<?> key : ConfigKeys.all()) {
            assertEquals(key.defaultValue(), reloaded.get(key), "默认值应完整回读：" + key.getPath());
        }
    }

    @Test
    @DisplayName("确定性：同一快照两次写出的文件内容完全相同")
    void writtenFilesAreIdentical(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        runtime.set(ConfigKeys.SERVER_NAME, "MyServer");

        ConfigWriteSnapshot snapshot = ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty());
        Path first = tempDir.resolve("a.yml");
        Path second = tempDir.resolve("b.yml");

        writer.write(snapshot, first, null);
        writer.write(snapshot, second, null);

        assertEquals(
                new String(Files.readAllBytes(first), StandardCharsets.UTF_8),
                new String(Files.readAllBytes(second), StandardCharsets.UTF_8),
                "同一快照必须产生完全相同的文件");
    }
}
