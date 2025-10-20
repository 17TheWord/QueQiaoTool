package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class BasePlayerChatEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testBasePlayerChatEventCreation() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer mockPlayer = new BasePlayer("TestPlayer", java.util.UUID.randomUUID());
        BasePlayerChatEvent event = new BasePlayerChatEvent("ChatEvent", "MsgID123", mockPlayer, "Hello, World!");
        assertNotNull(event);
    }

}