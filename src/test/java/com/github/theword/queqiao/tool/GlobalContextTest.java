package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlobalContextTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void setConfigShouldUpdateRuntimeConfig() {
        Config config = testConfig();

        GlobalContext.setConfig(config);

        assertSame(config, GlobalContext.getConfig());
    }

    @Test
    void setLoggerShouldUpdateRuntimeLogger() {
        GlobalContext.setLogger(logger);

        assertSame(logger, GlobalContext.getLogger());
    }

    @Test
    void initMessagePrefixJsonObjectShouldUseRuntimeImplementation() {
        prepareRuntime();

        JsonObject jsonObject = GlobalContext.initMessagePrefixJsonObject("QueQiao").getAsJsonObject();

        assertEquals("QueQiao", jsonObject.get("text").getAsString());
        assertEquals("yellow", jsonObject.get("color").getAsString());
    }

    private void prepareRuntime() {
        GlobalContext.setLogger(logger);
        GlobalContext.setConfig(testConfig());
    }

    private Config testConfig() {
        return new TestConfig(logger);
    }

    private static final class TestConfig extends Config {
        private TestConfig(Logger logger) {
            super(logger);
        }
    }
}
