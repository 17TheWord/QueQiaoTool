package io.github.theword.queqiao.core.utils;

import io.github.theword.queqiao.core.GlobalContext;
import io.github.theword.queqiao.core.config.Config;
import io.github.theword.queqiao.core.config.ConfigKeys;
import io.github.theword.queqiao.core.config.schema.ConfigRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Tool} 单元测试
 *
 * <p><b>测试隔离</b>：这里注入一份<b>不触碰文件系统</b>的默认配置
 * （由 {@link ConfigKeys} 的 Schema 直接构造），因此不读写工作目录下的
 * {@code plugins/queqiao/config.yml}。
 */
class ToolTest {

    @Test
    @DisplayName("忽略命令判定：默认忽略注册与登录命令")
    void testisIgnoredCommand() {
        // 默认值只来自 ConfigKeys 的 Schema（Phase 7 §40）
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        GlobalContext.setConfig(new Config(registry));

        assertEquals("", Tool.isIgnoredCommand("/login test"));
        assertEquals("", Tool.isIgnoredCommand("login test"));
        assertEquals("", Tool.isIgnoredCommand("/register test"));
        assertEquals("", Tool.isIgnoredCommand("register test"));
        assertEquals("", Tool.isIgnoredCommand("/l test"));
        assertEquals("", Tool.isIgnoredCommand("l test"));
        assertEquals("", Tool.isIgnoredCommand("/reg test"));
        assertEquals("", Tool.isIgnoredCommand("reg test"));
        assertEquals("other test", Tool.isIgnoredCommand("other test"));
        assertEquals("other test", Tool.isIgnoredCommand("/other test"));
    }
}
