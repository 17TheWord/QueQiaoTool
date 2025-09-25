package com.github.theword.queqiao.tool.config;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConfigTest {

  Logger logger = LoggerFactory.getLogger(getClass());

  @Test
  void loadConfig() {
    Config config = Config.loadConfig(false, logger);
    logger.info("config: {}", config);
  }
}
