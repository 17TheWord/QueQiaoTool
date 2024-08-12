package queqiao.utils;

import com.github.theword.queqiao.tool.utils.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static com.github.theword.queqiao.tool.utils.Tool.logger;

class ConfigTest {


    @Test
    void testLoadConfigWithException() {
        logger = LoggerFactory.getLogger(ConfigTest.class);
        Config config = Config.loadConfig(false);
        logger.info(config.getServer_name());
    }
}
