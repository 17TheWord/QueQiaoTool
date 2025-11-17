package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ToolTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testisIgnoredCommand() {
        GlobalContext.setConfig(Config.loadConfig(false, logger));
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

