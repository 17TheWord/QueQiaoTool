package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class BasePlayerAdvancementEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testBasePlayerAdvancementEventCreation() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer mockPlayer = new BasePlayer("TestPlayer", java.util.UUID.randomUUID());
        BasePlayerAdvancementEvent.BaseAdvancement advancement = new BasePlayerAdvancementEvent.BaseAdvancement("TestDescription");
        BasePlayerAdvancementEvent event = new BasePlayerAdvancementEvent("TestAdvancement", mockPlayer, advancement);
        assertNotNull(event);
    }

}