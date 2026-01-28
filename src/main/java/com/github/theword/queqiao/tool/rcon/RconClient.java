package com.github.theword.queqiao.tool.rcon;

import org.glavo.rcon.AuthenticationException;
import org.glavo.rcon.Rcon;
import org.slf4j.Logger;

import java.io.IOException;

public class RconClient {

    private final Logger logger;
    public volatile Rcon client;

    private int port;
    private String password;

    public RconClient(Logger logger, int port, String password) {
        this.logger = logger;
        this.port = port;
        this.password = password;
    }

    /**
     * 尝试连接 Rcon，返回是否成功
     */
    public void connect() {
        if (isConnected()) {
            logger.warn("Rcon 已连接，无需重复连接");
        }
        try {
            client = new Rcon("localhost", port, password);
            logger.info("Rcon 连接成功！[port: {}]", port);
        } catch (AuthenticationException e) {
            logger.error("Rcon 认证失败，请检查配置项是否正确");
        } catch (IOException e) {
            logger.warn("Rcon 连接失败：", e);
        }
    }

    public String sendCommand(String command) throws IOException {
        if (!isConnected()) {
            throw new IllegalArgumentException("Rcon 未连接");
        }
        return client.command(command);
    }

    /**
     * 关闭 Rcon 连接
     * <p> 1. 先判断 client 是否为 null </p>
     * <p> 2. 调用 client.close() </p>
     * <p> 3. 捕获异常并打印日志 </p>
     * <p> 4. 将 client 置为 null </p>
     * <p> 5. 打印关闭成功日志 </p>
     */
    public void stop() {
        if (client == null) return;

        Rcon tempClient = client; // 建立局部引用
        client = null; // 立即将全局引用置空，让 isConnected() 瞬间失效

        try {
            logger.info("正在关闭 Rcon 客户端...");
            tempClient.close();
        } catch (Exception e) {
            logger.warn("Rcon 关闭过程中发生异常: {}", e.getMessage());
        }
        logger.info("Rcon 客户端资源已释放");
    }

    public boolean isConnected() {
        return client != null;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
