package com.github.theword.queqiao.tool.rcon;

import com.github.theword.queqiao.tool.config.RconConfig;
import org.glavo.rcon.RconClient;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Rcon 客户端封装，支持自动重连与命令发送 */
public class RconClient {
    private final Logger logger;
    private final RconConfig config;
    private final AtomicInteger reconnectTimes = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile RconClient client;
    private volatile boolean stopped = false;

    public RconClient(RconConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * 连接 Rcon 服务器
     */
    public void connect() {
        if (!config.isEnable())
            return;
        try {
            client = RconClient.connect(config.getHost(), config.getPort(), config.getPassword());
            reconnectTimes.set(0);
            logger.info("Rcon 连接成功: {}:{}", config.getHost(), config.getPort());
        } catch (IOException e) {
            logger.warn("Rcon 连接失败: {}:{}", config.getHost(), config.getPort(), e);
            scheduleReconnect(nextDelay());
        }
    }

    /**
     * 发送命令
     */
    public String sendCommand(String command) throws IOException {
        if (client == null)
            throw new IOException("Rcon 未连接");
        return client.sendCommand(command);
    }

    private long nextDelay() {
        return Math.min(config.getReconnectInterval() * (1L << reconnectTimes.get()), 60);
    }

    private void scheduleReconnect(long delaySeconds) {
        if (stopped || reconnectTimes.get() >= config.getReconnectMaxTimes()) {
            logger.info("Rcon 达到最大重连次数: {}:{}", config.getHost(), config.getPort());
            return;
        }
        reconnectTimes.incrementAndGet();
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * 主动断开并停止重连
     */
    public void stop() {
        stopped = true;
        scheduler.shutdownNow();
        if (client != null)
            client.close();
    }

    /**
     * 判断是否已连接
     */
    public boolean isConnected() {
        return client != null && client.isOpen();
    }
}
