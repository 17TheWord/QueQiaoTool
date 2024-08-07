package queqiao.utils;

import com.github.theword.queqiao.tool.utils.Config;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

class ConfigTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigTest.class);

    @Test
    void testLoadConfig() {
        Config config;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            Yaml yaml = new Yaml();
            config = yaml.loadAs(inputStream, Config.class);
            System.out.println(config.getServer_name());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

    @Test
    void testLoadConfigWithException() {
        Config config;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            Yaml yaml = new Yaml();
            config = yaml.loadAs(inputStream, Config.class);
            System.out.println(config.getServer_name());
            throw new RuntimeException("Test config with RuntimeException.");
        } catch (Exception e) {
            config = new Config();
            System.out.println(config.getServer_name());
            logger.warn(e.getMessage());
        }
    }
}
