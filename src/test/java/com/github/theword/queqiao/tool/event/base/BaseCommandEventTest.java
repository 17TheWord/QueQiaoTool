package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaseCommandEventTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void printBaseCommandEvent() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
        BasePlayer player = new BasePlayer("Alex", UUID.randomUUID());
        BaseCommandEvent event = new BaseCommandEvent("cmd", "msgid456", player, "/help");
        assertNotNull(event);
    }
}
