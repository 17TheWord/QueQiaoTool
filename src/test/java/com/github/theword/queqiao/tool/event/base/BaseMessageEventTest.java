package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.GlobalContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaseMessageEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void printBaseMessageEvent() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer player = new BasePlayer("Alex", UUID.randomUUID());
        BaseMessageEvent event = new BaseMessageEvent("chat", "player", "msgid123", player, "hello");
        assertNotNull(event);
    }
}
