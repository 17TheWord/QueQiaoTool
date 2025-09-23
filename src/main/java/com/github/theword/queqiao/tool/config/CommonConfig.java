package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.GlobalContext;
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


public abstract class CommonConfig {

    /**
     * 读取配置文件
     *
     * @param configFolder 配置文件所在文件夹
     * @param fileName     配置文件名
     */
    protected void readConfigFile(String configFolder, String fileName) {
        Path configPath = Paths.get("./" + configFolder, BaseConstant.MODULE_NAME, fileName);
        checkFileExists(configPath, fileName);
        readConfigValues(configPath, fileName);
    }

    /**
     * 读取配置文件内容
     *
     * @param path     路径
     * @param fileName 文件名
     */
    protected void readConfigValues(Path path, String fileName) {
        GlobalContext.getLogger().info("正在读取配置文件 {}...", fileName);
        try {
            Yaml yaml = new Yaml();
            Reader reader = Files.newBufferedReader(path);
            Map<String, Object> configMap = yaml.load(reader);
            loadConfigValues(configMap);
            GlobalContext.getLogger().info("读取配置文件 {} 成功。", fileName);
        } catch (IOException exception) {
            GlobalContext.getLogger().warn("读取配置文件 {} 失败。", fileName);
            GlobalContext.getLogger().warn(exception.getMessage());
            GlobalContext.getLogger().warn("将直接使用默认配置项。");
        }
    }

    /**
     * 加载配置文件内容
     * <p>由原版端实现，读取自定义 regex.yml</p>
     *
     * @param configMap 配置文件内容
     */
    protected abstract void loadConfigValues(Map<String, Object> configMap);

    /**
     * 检查配置文件是否存在
     *
     * @param path     路径
     * @param fileName 文件名
     */
    protected void checkFileExists(Path path, String fileName) {
        GlobalContext.getLogger().info("正在寻找配置文件 {}...", fileName);
        GlobalContext.getLogger().info("配置文件 {} 路径为：{}。", fileName, path.toAbsolutePath());
        if (Files.exists(path)) {
            GlobalContext.getLogger().info("配置文件 {} 已存在，将读取配置项。", fileName);
        } else {
            GlobalContext.getLogger().warn("配置文件 {} 不存在，将生成默认配置文件。", fileName);
            try {
                InputStream inputStream = Config.class.getClassLoader().getResourceAsStream(fileName);
                assert inputStream != null;
                FileUtils.copyInputStreamToFile(inputStream, path.toFile());
                GlobalContext.getLogger().info("已生成默认配置文件。");
            } catch (IOException e) {
                GlobalContext.getLogger().warn("生成配置文件失败。");
            }
        }

    }

}
