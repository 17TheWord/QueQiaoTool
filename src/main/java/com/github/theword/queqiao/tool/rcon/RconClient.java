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
        if (client != null) {
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

    public String sendCommand(String command) {
        if (client == null) {
            logger.error("Rcon 未连接，无法发送命令");
            return "Rcon 未连接，无法发送命令";
        }
        String result;
        try {
            result = client.command(command);
        } catch (IllegalArgumentException e) {
            logger.error("Rcon 发送命令时出现异常：{}", e.getMessage(), e);
            result = e.getMessage();
        } catch (IOException e) {
            logger.error("RCON 出现IO异常：", e);
            result = e.getMessage();
        }
        return result;
    }

    public void stop() {
        if (client == null) {
            logger.info("Rcon 未连接，无需关闭");
            return;
        }

        try {
            logger.info("正在关闭 Rcon 客户端...");
            client.close();
            logger.info("Rcon 客户端已关闭");
        } catch (IOException e) {
            logger.warn("Rcon 关闭失败", e);
        } catch (Exception e) {
            logger.warn("Rcon close() 发生未知异常", e);
        }
        client = null;
        logger.info("Rcon 已关闭");
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
