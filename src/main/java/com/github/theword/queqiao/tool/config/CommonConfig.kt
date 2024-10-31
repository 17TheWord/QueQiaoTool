package com.github.theword.queqiao.tool.config

import com.github.theword.queqiao.tool.constant.BaseConstant
import com.github.theword.queqiao.tool.utils.Tool
import org.apache.commons.io.FileUtils
import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class CommonConfig {
    protected fun readConfigFile(configFolder: String, fileName: String) {
        val configPath = Paths.get("./$configFolder", BaseConstant.MODULE_NAME, fileName)
        checkFileExists(configPath, fileName)
        readConfigValues(configPath, fileName)
    }

    protected fun readConfigValues(path: Path?, fileName: String) {
        Tool.logger.info("正在读取配置文件 {}...", fileName)
        try {
            val yaml = Yaml()
            val reader: Reader = Files.newBufferedReader(path)
            val configMap = yaml.load<Map<String, Any>>(reader)
            loadConfigValues(configMap)
            Tool.logger.info("读取配置文件 {} 成功。", fileName)
        } catch (exception: IOException) {
            Tool.logger.warn("读取配置文件 {} 失败。", fileName)
            Tool.logger.warn(exception.message)
            Tool.logger.warn("将直接使用默认配置项。")
        }
    }

    protected abstract fun loadConfigValues(configMap: Map<String, Any>)

    protected fun checkFileExists(path: Path, fileName: String) {
        Tool.logger.info("正在寻找配置文件 {}...", fileName)
        Tool.logger.info("配置文件 {} 路径为：{}。", fileName, path.toAbsolutePath())
        if (Files.exists(path)) {
            Tool.logger.info("配置文件 {} 已存在，将读取配置项。", fileName)
        } else {
            Tool.logger.warn("配置文件 {} 不存在，将生成默认配置文件。", fileName)
            try {
                val inputStream = Config::class.java.classLoader.getResourceAsStream(fileName)!!
                FileUtils.copyInputStreamToFile(inputStream, path.toFile())
                Tool.logger.info("已生成默认配置文件。")
            } catch (e: IOException) {
                Tool.logger.warn("生成配置文件失败。")
            }
        }
    }
}
