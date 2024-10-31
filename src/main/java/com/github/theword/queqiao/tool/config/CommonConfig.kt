package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.github.theword.queqiao.tool.utils.Tool.logger;

public abstract class CommonConfig {

    protected void readConfigFile(String configFolder, String fileName) {
        Path configPath = Paths.get("./" + configFolder, BaseConstant.MODULE_NAME, fileName);
        checkFileExists(configPath, fileName);
        readConfigValues(configPath, fileName);
    }

    protected void readConfigValues(Path path, String fileName) {
        logger.info("正在读取配置文件 {}...", fileName);
        try {
            Yaml yaml = new Yaml();
            Reader reader = Files.newBufferedReader(path);
            Map<String, Object> configMap = yaml.load(reader);
            loadConfigValues(configMap);
            logger.info("读取配置文件 {} 成功。", fileName);
        } catch (IOException exception) {
            logger.warn("读取配置文件 {} 失败。", fileName);
            logger.warn(exception.getMessage());
            logger.warn("将直接使用默认配置项。");
        }
    }

    protected abstract void loadConfigValues(Map<String, Object> configMap);

    protected void checkFileExists(Path path, String fileName) {
        logger.info("正在寻找配置文件 {}...", fileName);
        logger.info("配置文件 {} 路径为：{}。", fileName, path.toAbsolutePath());
        if (Files.exists(path)) {
            logger.info("配置文件 {} 已存在，将读取配置项。", fileName);
        } else {
            logger.warn("配置文件 {} 不存在，将生成默认配置文件。", fileName);
            try {
                InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(fileName);
                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, path.toFile());
                logger.info("已生成默认配置文件。");
            } catch (IOException e) {
                logger.warn("生成配置文件失败。");
            }
        }

    }

}
