package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 服务器状态采集工具
 *
 * <p>用于 {@code get_status} 接口，包含以下能力：</p>
 * <p>1. 启动时读取并缓存 {@code server.properties} 的服务器地址</p>
 * <p>2. 接口调用时执行一次 Minecraft Server List Ping</p>
 * <p>3. 采集 CPU 与内存信息并组装返回数据</p>
 */
public final class ServerStatusCollector {
    private static final int DEFAULT_SERVER_PORT = 25565;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int SOCKET_TIMEOUT_MILLIS = 3000;
    private static final int HANDSHAKE_PROTOCOL_VERSION = -1;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final Path[] REGEX_CONFIG_CANDIDATES = new Path[]{Paths.get("config", BaseConstant.MODULE_NAME, "regex.yml"), Paths.get(BaseConstant.MODULE_NAME, "regex.yml")
    };

    private static volatile PingTarget pingTarget = PingTarget.unavailable(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);

    private ServerStatusCollector() {
    }

    private static final class PingTarget {
        private final String host;
        private final int port;
        private final boolean available;

        private PingTarget(String host, int port, boolean available) {
            this.host = host;
            this.port = port;
            this.available = available;
        }

        private static PingTarget unavailable(String host, int port) {
            return new PingTarget(host, port, false);
        }
    }

    /**
     * 读取并缓存 Minecraft 服务器地址
     *
     * <p>优先从 {@code ./config/QueQiao/regex.yml} 的 {@code log_path} 获取服务端根目录，</p>
     * <p>再读取该目录下的 {@code server.properties}</p>
     *
     * @param logger 日志实现
     */
    public static void initPingTarget(Logger logger) {
        Path workingDirectory = Paths.get("").toAbsolutePath().normalize();
        Path serverPropertiesPath = resolveServerPropertiesPath(workingDirectory, logger);
        initPingTarget(serverPropertiesPath, logger);
    }

    static Path resolveServerPropertiesPath(Path workingDirectory, Logger logger) {
        Path absoluteWorkingDirectory = workingDirectory.toAbsolutePath().normalize();
        for (Path relativeRegexPath : REGEX_CONFIG_CANDIDATES) {
            Path regexConfigPath = absoluteWorkingDirectory.resolve(relativeRegexPath).normalize();
            if (!Files.isRegularFile(regexConfigPath)) {
                continue;
            }

            String logPath = readLogPath(regexConfigPath, logger);
            if (logPath == null || logPath.trim().isEmpty()) {
                continue;
            }

            Path serverRoot = inferServerRoot(regexConfigPath, logPath, absoluteWorkingDirectory, logger);
            if (serverRoot == null) {
                continue;
            }

            Path serverPropertiesPath = serverRoot.resolve("server.properties").normalize();
            logger.info("已通过 regex.yml 获取 server.properties 路径：{}", serverPropertiesPath);
            return serverPropertiesPath;
        }

        Path fallbackPath = absoluteWorkingDirectory.resolve("server.properties").normalize();
        logger.info("未找到可用 regex.yml，回退到默认 server.properties 路径：{}", fallbackPath);
        return fallbackPath;
    }

    private static String readLogPath(Path regexConfigPath, Logger logger) {
        logger.info("检测到原版端 regex 配置：{}", regexConfigPath);
        try (InputStream inputStream = Files.newInputStream(regexConfigPath)) {
            Yaml yaml = new Yaml();
            Object yamlObject = yaml.load(inputStream);
            if (!(yamlObject instanceof Map)) {
                logger.warn("regex.yml 内容不是 Map 结构，无法读取 log_path：{}", regexConfigPath);
                return null;
            }

            Object logPathObject = ((Map<?, ?>) yamlObject).get("log_path");
            if (!(logPathObject instanceof String)) {
                logger.warn("regex.yml 未配置 log_path，无法获取 server.properties：{}", regexConfigPath);
                return null;
            }

            String logPath = ((String) logPathObject).trim();
            if (logPath.isEmpty()) {
                logger.warn("regex.yml 的 log_path 为空，无法获取 server.properties：{}", regexConfigPath);
                return null;
            }
            return logPath;
        } catch (Exception e) {
            logger.warn("读取 regex.yml 失败，无法获取 server.properties：{}，错误：{}", regexConfigPath, e.getMessage());
            return null;
        }
    }

    private static Path inferServerRoot(Path regexConfigPath, String logPath, Path workingDirectory, Logger logger) {
        final Path logPathObject;
        try {
            logPathObject = Paths.get(logPath);
        } catch (Exception e) {
            logger.warn("log_path 非法，无法获取 server.properties：{}，错误：{}", logPath, e.getMessage());
            return null;
        }

        if (logPathObject.isAbsolute()) {
            return extractServerRootFromLogPath(logPathObject.normalize());
        }

        Path regexBasedRoot = deriveRootFromRegexConfig(regexConfigPath);
        Path[] rootCandidates = new Path[]{regexBasedRoot, workingDirectory
        };

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

            Path serverPropertiesPath = inferredRoot.resolve("server.properties").normalize();
            if (Files.isRegularFile(serverPropertiesPath)) {
                return inferredRoot;
            }
        }

        if (firstInferredRoot != null) {
            logger.warn("已根据 log_path 获取服务器根目录 {}，但未找到 server.properties，将继续按该路径尝试", firstInferredRoot);
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

    static void initPingTarget(Path serverPropertiesPath, Logger logger) {
        String host = DEFAULT_SERVER_HOST;
        int port = DEFAULT_SERVER_PORT;
        Path normalizedPath = serverPropertiesPath.toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalizedPath)) {
            logger.warn("未找到 server.properties，状态接口将使用默认地址 {}:{}，当前路径：{}", host, port, normalizedPath);
            setPingTarget(host, port, false);
            return;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(normalizedPath)) {
            properties.load(inputStream);
            String configuredHost = properties.getProperty("server-ip", "").trim();
            String configuredPort = properties.getProperty("server-port", String.valueOf(DEFAULT_SERVER_PORT)).trim();

            if (!configuredHost.isEmpty()) {
                host = configuredHost;
            }
            port = parsePort(configuredPort, logger);
        } catch (IOException e) {
            logger.warn("读取 server.properties 失败，状态接口将使用默认地址 {}:{}，错误：{}", host, port, e.getMessage());
            setPingTarget(host, port, false);
            return;
        }

        setPingTarget(host, port, true);
        logger.info("状态接口已缓存 Minecraft 服务器地址 {}:{}（来源：{}）", host, port, normalizedPath);
    }

    private static void setPingTarget(String host, int port, boolean available) {
        pingTarget = new PingTarget(host, port, available);
    }

    private static int parsePort(String portText, Logger logger) {
        try {
            int parsedPort = Integer.parseInt(portText);
            if (parsedPort <= 0 || parsedPort > 65535) {
                logger.warn("server-port 配置越界（{}），将使用默认端口 {}", portText, DEFAULT_SERVER_PORT);
                return DEFAULT_SERVER_PORT;
            }
            return parsedPort;
        } catch (NumberFormatException e) {
            logger.warn("server-port 配置非法（{}），将使用默认端口 {}", portText, DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }
    }

    /**
     * 采集一次 {@code get_status} 响应数据
     *
     * @return 接口 data 字段
     */
    public static Map<String, Object> collectStatusSnapshot() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("timestamp", System.currentTimeMillis());
        data.put("server_type", GlobalContext.getServerType());
        data.put("server_version", GlobalContext.getServerVersion());
        data.put("server_list_ping", collectServerListPing());
        data.put("cpu_information", collectCpuInformation());
        data.put("memory_information", collectMemoryInformation());
        return data;
    }

    private static Map<String, Object> collectServerListPing() {
        PingTarget currentTarget = pingTarget;
        if (!currentTarget.available) {
            return null;
        }

        try {
            return pingMinecraftServer(currentTarget.host, currentTarget.port);
        } catch (Exception e) {
            Logger logger = GlobalContext.getLogger();
            if (logger != null) {
                logger.warn("Minecraft Server List Ping failed, server_list_ping will be null: {}", e.getMessage());
            }
            return null;
        }
    }

    private static Map<String, Object> pingMinecraftServer(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT_MILLIS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            sendHandshakePacket(outputStream, host, port);
            sendStatusRequestPacket(outputStream);

            int responsePacketLength = readVarInt(inputStream);
            if (responsePacketLength <= 0) {
                throw new IOException("状态响应包长度非法: " + responsePacketLength);
            }

            int packetId = readVarInt(inputStream);
            if (packetId != 0x00) {
                throw new IOException("状态响应包 ID 非法: " + packetId);
            }

            int jsonLength = readVarInt(inputStream);
            if (jsonLength <= 0) {
                throw new IOException("状态响应 JSON 长度非法: " + jsonLength);
            }

            byte[] jsonBytes = readFully(inputStream, jsonLength);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            Map<String, Object> pingData = GlobalContext.getGson().fromJson(json, MAP_TYPE);
            if (pingData == null) {
                throw new IOException("状态响应 JSON 解析失败");
            }
            return pingData;
        }
    }

    private static void sendHandshakePacket(OutputStream outputStream, String host, int port) throws IOException {
        ByteArrayOutputStream handshakeBody = new ByteArrayOutputStream();
        writeVarInt(handshakeBody, 0x00);
        writeVarInt(handshakeBody, HANDSHAKE_PROTOCOL_VERSION);
        writeString(handshakeBody, host);
        writeUnsignedShort(handshakeBody, port);
        writeVarInt(handshakeBody, 0x01);
        writePacket(outputStream, handshakeBody.toByteArray());
    }

    private static void sendStatusRequestPacket(OutputStream outputStream) throws IOException {
        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        writeVarInt(requestBody, 0x00);
        writePacket(outputStream, requestBody.toByteArray());
    }

    private static void writePacket(OutputStream outputStream, byte[] packetBody) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        writeVarInt(packet, packetBody.length);
        packet.write(packetBody);
        outputStream.write(packet.toByteArray());
        outputStream.flush();
    }

    private static void writeString(OutputStream outputStream, String text) throws IOException {
        byte[] value = text.getBytes(StandardCharsets.UTF_8);
        writeVarInt(outputStream, value.length);
        outputStream.write(value);
    }

    private static void writeUnsignedShort(OutputStream outputStream, int value) throws IOException {
        outputStream.write((value >>> 8) & 0xFF);
        outputStream.write(value & 0xFF);
    }

    private static void writeVarInt(OutputStream outputStream, int value) throws IOException {
        int current = value;
        while (true) {
            if ((current & 0xFFFFFF80) == 0) {
                outputStream.write(current);
                return;
            }

            outputStream.write((current & 0x7F) | 0x80);
            current >>>= 7;
        }
    }

    private static int readVarInt(InputStream inputStream) throws IOException {
        int numRead = 0;
        int result = 0;
        int read;

        do {
            read = inputStream.read();
            if (read == -1) {
                throw new EOFException("读取 VarInt 时连接提前关闭");
            }

            int value = read & 0x7F;
            result |= value << (7 * numRead);
            numRead++;

            if (numRead > 5) {
                throw new IOException("VarInt 过长");
            }
        } while ((read & 0x80) != 0);

        return result;
    }

    private static byte[] readFully(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int readCount = inputStream.read(data, offset, length - offset);
            if (readCount == -1) {
                throw new EOFException("读取状态响应时连接提前关闭");
            }
            offset += readCount;
        }
        return data;
    }

    private static Map<String, Object> collectCpuInformation() {
        Map<String, Object> cpuInformation = new LinkedHashMap<>();
        java.lang.management.OperatingSystemMXBean baseBean = ManagementFactory.getOperatingSystemMXBean();

        cpuInformation.put("cpu_cores", Runtime.getRuntime().availableProcessors());
        cpuInformation.put("load_average", round(baseBean.getSystemLoadAverage()));
        cpuInformation.put("system_load", toPercent(invokeDoubleGetter(baseBean, "getSystemCpuLoad", "getCpuLoad")));
        cpuInformation.put("process_load", toPercent(invokeDoubleGetter(baseBean, "getProcessCpuLoad")));

        return cpuInformation;
    }

    private static Map<String, Object> collectMemoryInformation() {
        Map<String, Object> memoryInformation = new LinkedHashMap<>();
        java.lang.management.OperatingSystemMXBean baseBean = ManagementFactory.getOperatingSystemMXBean();

        long physicalTotal = invokeLongGetter(baseBean, "getTotalPhysicalMemorySize", "getTotalMemorySize");
        long physicalFree = invokeLongGetter(baseBean, "getFreePhysicalMemorySize", "getFreeMemorySize");

        long physicalUsed = (physicalTotal >= 0 && physicalFree >= 0) ? (physicalTotal - physicalFree) : -1L;
        Map<String, Object> physicalMemory = new LinkedHashMap<>();
        physicalMemory.put("total", physicalTotal);
        physicalMemory.put("free", physicalFree);
        physicalMemory.put("used", physicalUsed);
        physicalMemory.put("percentage", calculatePercentage(physicalUsed, physicalTotal));

        Runtime runtime = Runtime.getRuntime();
        long jvmTotal = runtime.totalMemory();
        long jvmFree = runtime.freeMemory();
        long jvmMax = runtime.maxMemory();
        long jvmUsed = jvmTotal - jvmFree;
        Map<String, Object> jvmMemory = new LinkedHashMap<>();
        jvmMemory.put("total", jvmTotal);
        jvmMemory.put("free", jvmFree);
        jvmMemory.put("max", jvmMax);
        jvmMemory.put("used", jvmUsed);
        jvmMemory.put("percentage", calculatePercentage(jvmUsed, jvmMax));

        memoryInformation.put("physical_memory", physicalMemory);
        memoryInformation.put("jvm_memory", jvmMemory);
        return memoryInformation;
    }

    private static double invokeDoubleGetter(Object object, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        return -1D;
    }

    private static long invokeLongGetter(Object object, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            } catch (Exception ignored) {
            }
        }
        return -1L;
    }

    private static double toPercent(double value) {
        if (value < 0) {
            return -1D;
        }
        return round(value * 100);
    }

    private static double calculatePercentage(long used, long total) {
        if (used < 0 || total <= 0) {
            return -1D;
        }
        return round((used * 100D) / total);
    }

    private static double round(double value) {
        if (value < 0) {
            return -1D;
        }
        return Math.round(value * 100D) / 100D;
    }
}
