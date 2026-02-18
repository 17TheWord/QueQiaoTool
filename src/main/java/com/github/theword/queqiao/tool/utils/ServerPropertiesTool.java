package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * server.properties 读取工具。
 *
 * <p>类加载时会完成一次路径推断和文件读取，并将结果缓存到内存。</p>
 * <p>后续调用直接命中缓存，避免反复 IO。</p>
 */
public final class ServerPropertiesTool {
    private static final String LOG_PATH_KEY = "log_path";
    private static final String SERVER_PROPERTIES_FILE_NAME = "server.properties";

    private static final Path[] REGEX_CONFIG_CANDIDATES = new Path[]{Paths.get("config", BaseConstant.MODULE_NAME, "regex.yml"), Paths.get(BaseConstant.MODULE_NAME, "regex.yml")
    };
    private static final Path WORKING_DIRECTORY = Paths.get("").toAbsolutePath().normalize();
    private static volatile Snapshot snapshot = loadSnapshot(WORKING_DIRECTORY);

    private ServerPropertiesTool() {
    }

    /**
     * 获取 server.properties 绝对路径。
     */
    public static Path getServerPropertiesPath() {
        return snapshot.serverPropertiesPath;
    }

    /**
     * 刷新 server.properties 缓存。
     *
     * <p>在 reload 等场景下调用，可确保后续读取到最新配置。</p>
     */
    public static synchronized void refresh() {
        snapshot = loadSnapshot(WORKING_DIRECTORY);
    }

    /**
     * 通过 key 获取 server.properties 配置值。
     *
     * <p>找不到 key 或配置文件不存在时返回 null。</p>
     */
    public static String getValue(String key) {
        if (isBlank(key)) {
            return null;
        }
        return snapshot.properties.get(key);
    }

    private static Snapshot loadSnapshot(Path workingDirectory) {
        Path serverPropertiesPath = resolveServerPropertiesPath(workingDirectory);
        Map<String, String> properties = loadProperties(serverPropertiesPath);
        return new Snapshot(serverPropertiesPath, properties);
    }

    private static Map<String, String> loadProperties(Path serverPropertiesPath) {
        if (!Files.isRegularFile(serverPropertiesPath)) {
            return Collections.emptyMap();
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(serverPropertiesPath)) {
            properties.load(inputStream);
        } catch (Exception e) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            map.put(key, value == null ? null : value.trim());
        }
        return Collections.unmodifiableMap(map);
    }

    private static Path resolveServerPropertiesPath(Path workingDirectory) {
        Path absoluteWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        for (Path relativeRegexPath : REGEX_CONFIG_CANDIDATES) {
            Path regexConfigPath = absoluteWorkingDirectory.resolve(relativeRegexPath).normalize();
            if (!Files.isRegularFile(regexConfigPath)) {
                continue;
            }

            String logPath = readLogPath(regexConfigPath);
            if (isBlank(logPath)) {
                continue;
            }

            Path serverRoot = inferServerRoot(regexConfigPath, logPath, absoluteWorkingDirectory);
            if (serverRoot == null) {
                continue;
            }

            return serverRoot.resolve(SERVER_PROPERTIES_FILE_NAME).normalize();
        }

        return absoluteWorkingDirectory.resolve(SERVER_PROPERTIES_FILE_NAME).normalize();
    }

    private static String readLogPath(Path regexConfigPath) {
        try (InputStream inputStream = Files.newInputStream(regexConfigPath)) {
            Yaml yaml = new Yaml();
            Object yamlObject = yaml.load(inputStream);
            if (!(yamlObject instanceof Map)) {
                return null;
            }

            Object logPathObject = ((Map<?, ?>) yamlObject).get(LOG_PATH_KEY);
            if (!(logPathObject instanceof String)) {
                return null;
            }

            String logPath = ((String) logPathObject).trim();
            if (isBlank(logPath)) {
                return null;
            }
            return logPath;
        } catch (Exception e) {
            return null;
        }
    }

    private static Path inferServerRoot(Path regexConfigPath, String logPath, Path workingDirectory) {
        final Path logPathObject;
        try {
            logPathObject = Paths.get(logPath);
        } catch (Exception e) {
            return null;
        }

        if (logPathObject.isAbsolute()) {
            return extractServerRootFromLogPath(logPathObject.normalize());
        }

        Path regexBasedRoot = deriveRootFromRegexConfig(regexConfigPath);
        Path[] rootCandidates = new Path[]{regexBasedRoot, workingDirectory};

        Path firstInferredRoot = null;
        for (Path rootCandidate : rootCandidates) {
            if (rootCandidate == null) {
                continue;
            }

            Path resolvedLogPath = rootCandidate.resolve(logPathObject).normalize();
            Path inferredRoot = extractServerRootFromLogPath(resolvedLogPath);
            if (inferredRoot == null) {
                continue;
            }
            if (firstInferredRoot == null) {
                firstInferredRoot = inferredRoot;
            }

            Path serverPropertiesPath = inferredRoot.resolve(SERVER_PROPERTIES_FILE_NAME).normalize();
            if (Files.isRegularFile(serverPropertiesPath)) {
                return inferredRoot;
            }
        }
        return firstInferredRoot;
    }

    private static Path deriveRootFromRegexConfig(Path regexConfigPath) {
        Path normalizedPath = regexConfigPath.toAbsolutePath().normalize();
        Path moduleDirectory = normalizedPath.getParent();
        if (moduleDirectory == null) {
            return null;
        }

        Path moduleParent = moduleDirectory.getParent();
        if (moduleParent == null) {
            return null;
        }

        if (equalsIgnoreCase(moduleDirectory.getFileName(), BaseConstant.MODULE_NAME) && equalsIgnoreCase(moduleParent.getFileName(), "config")) {
            Path serverRoot = moduleParent.getParent();
            if (serverRoot != null) {
                return serverRoot;
            }
        }

        return moduleParent;
    }

    private static boolean equalsIgnoreCase(Path pathSegment, String text) {
        return pathSegment != null && pathSegment.toString().equalsIgnoreCase(text);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static Path extractServerRootFromLogPath(Path resolvedLogPath) {
        Path logDirectory = resolvedLogPath.getParent();
        if (logDirectory == null) {
            return null;
        }

        if (equalsIgnoreCase(logDirectory.getFileName(), "logs") && logDirectory.getParent() != null) {
            return logDirectory.getParent();
        }

        return logDirectory;
    }

    private static final class Snapshot {
        private final Path serverPropertiesPath;
        private final Map<String, String> properties;

        private Snapshot(Path serverPropertiesPath, Map<String, String> properties) {
            this.serverPropertiesPath = serverPropertiesPath;
            this.properties = properties;
        }
    }
}
