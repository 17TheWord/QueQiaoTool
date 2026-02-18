package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.websocket.WsClient;
import com.github.theword.queqiao.tool.websocket.WsServer;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class WebsocketManager {
    /**
     * Websocket Client 列表
     */
    private final List<WsClient> wsClientList;

    /**
     * Websocket Server
     */
    private WsServer wsServer;

    private final Logger logger;

    private final Gson gson;

    private final HandleCommandReturnMessageService handleCommandReturnMessageService;

    public List<WsClient> getWsClientList() {
        return wsClientList;
    }

    public WsServer getWsServer() {
        return wsServer;
    }

    public WebsocketManager(Logger logger, Gson gson, HandleCommandReturnMessageService handleCommandReturnMessageService) {
        this.logger = logger;
        this.gson = gson;
        this.handleCommandReturnMessageService = handleCommandReturnMessageService;
        this.wsClientList = new ArrayList<>();
    }

    /**
     * 启动 WebSocket 客户端 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private void startClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
        GlobalContext.getConfig().getWebsocketClient().getUrlList().forEach(
                websocketUrl -> {
                    try {
                        WsClient wsClient = new WsClient(
                                new URI(websocketUrl), logger, gson, GlobalContext.getConfig().getServerName(), GlobalContext.getConfig().getAccessToken(), GlobalContext.getConfig().getWebsocketClient().getReconnectMaxTimes(), GlobalContext.getConfig().getWebsocketClient().getReconnectInterval(), GlobalContext.getConfig().isEnable());
                        wsClient.connect();
                        wsClientList.add(wsClient);
                    } catch (URISyntaxException e) {
                        this.handleCommandReturnMessageService.sendReturnMessage(
                                commandReturner, String.format(
                                        WebsocketConstantMessage.Client.URI_SYNTAX_ERROR.replace("{}", "%s"), websocketUrl));
                    }
                });

    }

    /**
     * 停止 WebSocket 客户端 关闭原因至少需要传入一个带有 %s 的字符串来填入对应的 URI
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    private void stopClients(int code, String reason, Object commandReturner) {
        for (WsClient wsClient : wsClientList) {
            this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, String.format(reason, wsClient.getURI()));
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.getURI()));
        }
        wsClientList.clear();
        this.handleCommandReturnMessageService.sendReturnMessage(
                commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }

    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    private void restartClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        stopClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            startClients(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADED);
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     */
    private void startServer(Object commandReturner) {
        String host = GlobalContext.getConfig().getWebsocketServer().getHost();
        int port = GlobalContext.getConfig().getWebsocketServer().getPort();

        wsServer = new WsServer(
                new InetSocketAddress(host, port), logger, gson, GlobalContext.getConfig().getServerName(), GlobalContext.getConfig().getAccessToken(), GlobalContext.getConfig().isEnable(), GlobalContext.getConfig().getWebsocketServer().isForward()
        );
        try {
            wsServer.start();
        } catch (RuntimeException e) {
            wsServer = null;
            String errorMessage = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            this.logger.error("WebSocket Server 启动失败: {}", errorMessage, e);
            this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, String.format("WebSocket Server 启动失败：%s", errorMessage));
            return;
        }
        this.handleCommandReturnMessageService.sendReturnMessage(
                commandReturner, String.format(
                        WebsocketConstantMessage.Server.SERVER_STARTING.replace("{}", "%s"), wsServer.getAddress().getHostString(), wsServer.getPort()));

    }

    /**
     * 停止 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     * @param reason          原因
     */
    private void stopServer(Object commandReturner, String reason) {
        if (wsServer != null) {
            try {
                wsServer.stop(0, reason);
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, reason);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                this.logger.warn("停止 WebSocket Server 时线程被中断", e);
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
            }
            wsServer = null;
        }
    }

    /**
     * 重载 WebSocket 服务器 目前只有通过reload命令调用重载
     *
     * @param commandReturner 命令执行者
     */
    private void restartServer(Object commandReturner) {
        stopServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            startServer(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.RELOADED);
    }

    /**
     * 启动 WebSocket 开服时调用
     *
     * @param commandReturner 命令执行者
     */
    public void start(Object commandReturner) {
        if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            startClients(commandReturner);
        }
        if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            startServer(commandReturner);
        }
    }

    /**
     * 停止 WebSocket 除传入关闭码、关闭原因外，还需传入命令执行者（可为null）
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    public void stop(int code, String reason, Object commandReturner) {
        stopClients(code, reason, commandReturner);
        stopServer(commandReturner, reason);
    }

    /**
     * 重载 WebSocket 同时重载客户端和服务端
     *
     * @param commandReturner 命令执行者
     */
    public void restart(Object commandReturner) {
        restartClients(commandReturner);
        restartServer(commandReturner);
    }

    /**
     * 发送消息 同时向所有 Websocket 客户端和服务端广播消息
     *
     * @param event 任何继承于 BaseEvent 的事件
     */
    public void sendEvent(BaseEvent event) {
        if (GlobalContext.getConfig().isEnable()) {
            String json = gson.toJson(event);
            wsClientList.forEach(
                    wsClient -> {
                        if (wsClient.isOpen()) {
                            wsClient.send(json);
                            Tool.debugLog("WebSocket Client {} 发送消息: {}", wsClient.getURI(), json);
                        } else {
                            Tool.debugLog("WebSocket Client {} 未连接，跳过发送消息: {}", wsClient.getURI(), json);
                        }
                    });
            if (wsServer != null) {
                wsServer.broadcast(json);
                Tool.debugLog("WebSocket Server 广播消息: {}", json);
            }
        }
    }
}
