package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;


class ConfigTest {

    @Test
    void loadConfig() {
        GlobalContext.setLogger(LoggerFactory.getLogger(ConfigTest.class));
        GlobalContext.setConfig(Config.loadConfig(false));
        GlobalContext.getLogger().info("config: {}", GlobalContext.getConfig());
    }

}