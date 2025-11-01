package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaseNoticeEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testBaseNoticeEventCreation() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer mockPlayer = new BasePlayer("TestPlayer", UUID.randomUUID());
        BaseNoticeEvent event = new BaseNoticeEvent("TestEvent", "TestSubType", mockPlayer);
        assertNotNull(event);
    }

}