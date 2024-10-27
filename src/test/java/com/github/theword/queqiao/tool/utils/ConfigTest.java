package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.config.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;


import static com.github.theword.queqiao.tool.utils.Tool.config;
import static com.github.theword.queqiao.tool.utils.Tool.logger;


class ConfigTest {

    @Test
    void loadConfig() {
        logger = LoggerFactory.getLogger(ConfigTest.class);
        config = Config.loadConfig(false);
        logger.info("config: {}", config);
    }

}