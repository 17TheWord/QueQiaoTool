package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.config.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首次启动自动生成配置的集成测试（Phase 7 §35 / §36）
 *
 * <pre>
 * 无 config.yml
 *    ↓ 全部使用 Schema 默认值
 *    ↓ ConfigWriter 生成完整带注释的 config.yml
 *    ↓ 重新读取 → ConfigLoader
 * </pre>
 */
class GeneratedConfigIntegrationTest {

    @Test
    @DisplayName("首次启动生成完整配置，且能被 Loader 完整读回")
    void firstStartupGeneratesLoadableConfig(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        Path target = tempDir.resolve("config.yml");

        // 无文件 → 全默认值 → 生成
        new ConfigWriter().write(ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty()), target, null);
        assertTrue(Files.exists(target), "首次启动应生成 config.yml");

        String content = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        assertTrue(content.contains("#"), "生成的配置应带注释");
        assertTrue(content.contains("websocket_server:"), "生成的配置应含全部区块");

        // 重新读取 + 加载
        ConfigFileReader.Result read = ConfigFileReader.read(target);
        assertEquals(ConfigFileState.VALID, read.getState(), read.getErrorMessage());

        Config reloaded = new Config(registry);
        ConfigLoadResult loaded = new ConfigLoader(registry, reloaded).load(read.getState(), read.getMap());

        assertFalse(loaded.hasUnknown(), "生成的文件不应产生未知字段：" + loaded.getUnknownCorePaths());
        for (ConfigKey<?> key : ConfigKeys.all()) {
            assertEquals(runtime.get(key), reloaded.get(key), "生成的文件应完整回读：" + key.getPath());
        }
    }

    @Test
    @DisplayName("§36：默认值写入文件后读回，值不变但来源从 DEFAULT 变为 USER")
    void generatedDefaultsBecomeUserSourced(@TempDir Path tempDir) throws IOException {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        Config runtime = new Config(registry);
        Path target = tempDir.resolve("config.yml");

        // 生成前：全部来自默认值
        assertTrue(runtime.isDefault(ConfigKeys.SERVER_NAME));
        assertFalse(runtime.contains(ConfigKeys.SERVER_NAME));

        new ConfigWriter().write(ConfigWriteSnapshot.of(registry, runtime, ConfigDocument.empty()), target, null);

        ConfigFileReader.Result read = ConfigFileReader.read(target);
        Config reloaded = new Config(registry);
        new ConfigLoader(registry, reloaded).load(read.getState(), read.getMap());

        // 值不变
        assertEquals(runtime.get(ConfigKeys.SERVER_NAME), reloaded.get(ConfigKeys.SERVER_NAME));
        // 来源变为 USER —— 这是正确行为，不要试图用"值等于默认值"反推来源
        assertTrue(reloaded.contains(ConfigKeys.SERVER_NAME), "写入文件后读回应为 USER");
        assertFalse(reloaded.isDefault(ConfigKeys.SERVER_NAME));
    }
}
