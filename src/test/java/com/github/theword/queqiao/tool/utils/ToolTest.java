package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Tool} 单元测试
 *
 * <p><b>测试隔离</b>：配置通过 {@code Config.loadConfig(isModServer, logger, baseDirectory)}
 * 指向 {@link TempDir}，不再读写工作目录下的 {@code plugins/queqiao/config.yml}。
 */
class ToolTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @DisplayName("忽略命令判定：默认忽略注册与登录命令")
    void testisIgnoredCommand(@TempDir Path tempDir) {
        GlobalContext.setConfig(Config.loadConfig(false, logger, tempDir));

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
