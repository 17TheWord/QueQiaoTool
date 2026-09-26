package io.github.theword.queqiao.core.utils;

import io.github.theword.queqiao.core.config.Config;
import io.github.theword.queqiao.core.config.ConfigKeys;
import io.github.theword.queqiao.core.config.schema.ConfigRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RuntimeUtils} 单元测试
 *
 * <p><b>测试隔离</b>：这里注入一份<b>不触碰文件系统</b>的默认配置
 * （由 {@link ConfigKeys} 的 Schema 直接构造），因此不读写工作目录下的
 * {@code plugins/queqiao/config.yml}。
 */
class RuntimeUtilsTest {

    private static Config defaultConfig() {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        return new Config(registry);
    }

    private static RuntimeUtils newUtils(Config config) {
        return new RuntimeUtils(config, NOPLogger.NOP_LOGGER);
    }

    // ------------------------------------------------------------------
    // isIgnoredCommand：返回语义必须保持 String
    // ------------------------------------------------------------------

    @Test
    @DisplayName("忽略命令判定：默认忽略注册与登录命令，且返回 String 语义")
    void isIgnoredCommandKeepsStringSemantics() {
        RuntimeUtils utils = newUtils(defaultConfig());

        assertEquals("", utils.isIgnoredCommand("/login test"));
        assertEquals("", utils.isIgnoredCommand("login test"));
        assertEquals("", utils.isIgnoredCommand("/register test"));
        assertEquals("", utils.isIgnoredCommand("register test"));
        assertEquals("", utils.isIgnoredCommand("/l test"));
        assertEquals("", utils.isIgnoredCommand("l test"));
        assertEquals("", utils.isIgnoredCommand("/reg test"));
        assertEquals("", utils.isIgnoredCommand("reg test"));
        assertEquals("other test", utils.isIgnoredCommand("other test"));
        assertEquals("other test", utils.isIgnoredCommand("/other test"));
    }

    @Test
    @DisplayName("忽略命令判定：null 与空白返回空串")
    void isIgnoredCommandHandlesNullAndBlank() {
        RuntimeUtils utils = newUtils(defaultConfig());

        assertEquals("", utils.isIgnoredCommand(null));
        assertEquals("", utils.isIgnoredCommand(""));
        assertEquals("", utils.isIgnoredCommand("   "));
        assertEquals("", utils.isIgnoredCommand("/"));
    }

    @Test
    @DisplayName("配置变更后忽略命令自动生效，不缓存旧集合")
    void isIgnoredCommandFollowsConfigChanges() {
        Config config = defaultConfig();
        RuntimeUtils utils = newUtils(config);

        assertEquals("tp Steve", utils.isIgnoredCommand("tp Steve"), "初始未配置 tp，不应被忽略");

        config.set(ConfigKeys.IGNORED_COMMANDS, Arrays.asList("tp"));
        assertEquals("", utils.isIgnoredCommand("tp Steve"), "配置加入 tp 后应立即生效");

        config.set(ConfigKeys.IGNORED_COMMANDS, Collections.<String>emptyList());
        assertEquals("tp Steve", utils.isIgnoredCommand("tp Steve"), "配置移除 tp 后应立即失效");
    }

    // ------------------------------------------------------------------
    // debug 门控
    // ------------------------------------------------------------------

    @Test
    @DisplayName("debug 默认关闭，且关闭时 debugLog 不抛异常")
    void debugDisabledByDefault() {
        RuntimeUtils utils = newUtils(defaultConfig());

        assertFalse(utils.isDebugEnabled(), "DEBUG 的 Schema 默认值为 false");
        utils.debugLog("不应输出");
        utils.debugLog("不应输出 {} 与 {}", 1, 2);
    }

    @Test
    @DisplayName("debug 开启后 isDebugEnabled 为 true")
    void debugEnabledFollowsConfig() {
        Config config = defaultConfig();
        RuntimeUtils utils = newUtils(config);

        config.set(ConfigKeys.DEBUG, true);
        assertTrue(utils.isDebugEnabled(), "配置开启 debug 后应立即生效");

        config.set(ConfigKeys.DEBUG, false);
        assertFalse(utils.isDebugEnabled(), "配置关闭 debug 后应立即失效");
    }

    // ------------------------------------------------------------------
    // 构造器不变量
    // ------------------------------------------------------------------

    @Test
    @DisplayName("构造器拒绝 null 依赖")
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new RuntimeUtils(null, NOPLogger.NOP_LOGGER));
        assertThrows(NullPointerException.class, () -> new RuntimeUtils(defaultConfig(), null));
    }
}
