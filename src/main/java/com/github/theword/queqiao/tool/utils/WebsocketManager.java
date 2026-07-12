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
    private final Object lifecycleLock = new Object();
    private final List<WsClient> wsClientList;
    private volatile WsServer wsServer;
    private final Logger logger;
    private final Gson gson;
    private final HandleCommandReturnMessageService handleCommandReturnMessageService;

    public WebsocketManager(Logger logger, Gson gson, HandleCommandReturnMessageService handleCommandReturnMessageService) {
        this.logger = logger;
        this.gson = gson;
        this.handleCommandReturnMessageService = handleCommandReturnMessageService;
        this.wsClientList = new ArrayList<>();
    }

    public List<WsClient> getWsClientList() {
        synchronized (lifecycleLock) {
            return new ArrayList<>(wsClientList);
        }
    }

    public WsServer getWsServer() {
        synchronized (lifecycleLock) {
            return wsServer;
        }
    }

    private void startClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
        GlobalContext.getConfig().getWebsocketClient().getUrlList().forEach(
                websocketUrl -> {
                    try {
                        WsClient wsClient = new WsClient(
                                new URI(websocketUrl),
                                logger,
                                gson,
                                GlobalContext.getConfig().getServerName(),
                                GlobalContext.getConfig().getAccessToken(),
                                GlobalContext.getConfig().getWebsocketClient().getReconnectMaxTimes(),
                                GlobalContext.getConfig().getWebsocketClient().getReconnectInterval(),
                                GlobalContext.getConfig().isEnable()
                        );
                        wsClient.connect();
                        wsClientList.add(wsClient);
                    } catch (URISyntaxException e) {
                        this.handleCommandReturnMessageService.sendReturnMessage(
                                commandReturner,
                                String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR.replace("{}", "%s"), websocketUrl)
                        );
                    }
                });
    }

    private void stopClients(int code, String reason, Object commandReturner) {
        for (WsClient wsClient : wsClientList) {
            String closeReason = String.format(reason, wsClient.getURI());
            this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, closeReason);
            wsClient.stopWithoutReconnect(code, closeReason);
        }
        wsClientList.clear();
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }

    private void restartClients(Object commandReturner) {
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        stopClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            startClients(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Client.RELOADED);
    }

    private void startServer(Object commandReturner) {
        wsServer = new WsServer(
                new InetSocketAddress(
                        GlobalContext.getConfig().getWebsocketServer().getHost(),
                        GlobalContext.getConfig().getWebsocketServer().getPort()
                ),
                logger,
                gson,
                GlobalContext.getConfig().getServerName(),
                GlobalContext.getConfig().getAccessToken(),
                GlobalContext.getConfig().isEnable()
        );
        wsServer.start();
        this.handleCommandReturnMessageService.sendReturnMessage(
                commandReturner,
                String.format(
                        WebsocketConstantMessage.Server.SERVER_STARTING.replace("{}", "%s"),
                        GlobalContext.getConfig().getWebsocketServer().getHost(),
                        GlobalContext.getConfig().getWebsocketServer().getPort()
                )
        );
    }

    private void stopServer(Object commandReturner, String reason) {
        if (wsServer != null) {
            try {
                wsServer.stop(0, reason);
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, reason);
            } catch (InterruptedException e) {
                this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                Tool.debugLog(e.getMessage());
            }
            wsServer = null;
        }
    }

    private void restartServer(Object commandReturner) {
        stopServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            startServer(commandReturner);
        }
        this.handleCommandReturnMessageService.sendReturnMessage(commandReturner, WebsocketConstantMessage.Server.RELOADED);
    }

    public void start(Object commandReturner) {
        synchronized (lifecycleLock) {
            if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
                startClients(commandReturner);
            }
            if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
                startServer(commandReturner);
            }
        }
    }

    public void stop(int code, String reason, Object commandReturner) {
        synchronized (lifecycleLock) {
            stopClients(code, reason, commandReturner);
            stopServer(commandReturner, reason);
        }
    }

    public void restart(Object commandReturner) {
        synchronized (lifecycleLock) {
            restartClients(commandReturner);
            restartServer(commandReturner);
        }
    }

    public void sendEvent(BaseEvent event) {
        if (!GlobalContext.getConfig().isEnable()) {
            return;
        }

        String json = gson.toJson(event);
        List<WsClient> wsClientSnapshot;
        WsServer wsServerSnapshot;
        synchronized (lifecycleLock) {
            wsClientSnapshot = new ArrayList<>(wsClientList);
            wsServerSnapshot = wsServer;
        }

        wsClientSnapshot.forEach(wsClient -> sendClientEvent(wsClient, json));
        if (wsServerSnapshot != null) {
            broadcastServerEvent(wsServerSnapshot, json);
        }
    }

    private void sendClientEvent(WsClient wsClient, String json) {
        try {
            if (wsClient.isOpen()) {
                wsClient.send(json);
                Tool.debugLog("WebSocket Client {} send message {}", wsClient.getURI(), json);
            } else {
                Tool.debugLog("WebSocket Client {} is not connected, skip message {}", wsClient.getURI(), json);
            }
        } catch (RuntimeException e) {
            logger.warn("WebSocket Client send failed, uri={}, error={}", wsClient.getURI(), e.getMessage());
        }
    }

    private void broadcastServerEvent(WsServer server, String json) {
        try {
            server.broadcast(json);
            Tool.debugLog("WebSocket Server broadcast message: {}", json);
        } catch (RuntimeException e) {
            logger.warn("WebSocket Server broadcast failed, error={}", e.getMessage());
        }
    }
}
