package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BasePlayerDeathEventTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testBasePlayerDeathEventCreation() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer mockPlayer = new BasePlayer("TestPlayer", java.util.UUID.randomUUID());
        BasePlayerDeathEvent event = new BasePlayerDeathEvent("DeathEvent", "MsgID456", mockPlayer, "slain by Zombie");
        logger.info("BasePlayerDeathEvent: {}", event);
        assertNotNull(event);
    }
}

