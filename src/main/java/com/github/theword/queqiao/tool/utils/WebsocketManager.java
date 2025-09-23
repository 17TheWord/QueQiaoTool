package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.websocket.WsClient;
import com.github.theword.queqiao.tool.websocket.WsServer;
import com.google.gson.Gson;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


public class WebsocketManager {
    /**
     * Websocket Client 列表
     */
    private final List<WsClient> wsClientList = new ArrayList<>();

    /**
     * Websocket Server
     */
    private WsServer wsServer;

    private final Gson gson = GsonUtils.buildGson();

    public List<WsClient> getWsClientList() {
        return wsClientList;
    }

    public WsServer getWsServer() {
        return wsServer;
    }

    /**
     * 启动 WebSocket 客户端
     * 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketClients(Object commandReturner) {
        if (GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
            GlobalContext.getConfig().getWebsocketClient().getUrlList().forEach(websocketUrl -> {
                try {
                    WsClient wsClient = new WsClient(new URI(websocketUrl));
                    wsClient.connect();
                    wsClientList.add(wsClient);
                } catch (URISyntaxException e) {
                    Tool.commandReturn(commandReturner, String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl));
                }
            });
        }
    }

    /**
     * 停止 WebSocket 客户端
     * 关闭原因至少需要传入一个带有 %s 的字符串来填入对应的 URI
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    private void stopWebsocketClients(int code, String reason, Object commandReturner) {
        for (WsClient wsClient : wsClientList) {
            Tool.commandReturn(commandReturner, String.format(reason, wsClient.getURI()));
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.getURI()));
        }
        wsClientList.clear();
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }


    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    public void restartWebsocketClients(Object commandReturner) {
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        stopWebsocketClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        startWebsocketClients(commandReturner);
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADED);
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketServer(Object commandReturner) {
        if (GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            wsServer = new WsServer(new InetSocketAddress(GlobalContext.getConfig().getWebsocketServer().getHost(), GlobalContext.getConfig().getWebsocketServer().getPort()));
            wsServer.start();
            Tool.commandReturn(commandReturner, String.format(WebsocketConstantMessage.Server.SERVER_STARTING, GlobalContext.getConfig().getWebsocketServer().getHost(), GlobalContext.getConfig().getWebsocketServer().getPort()));
        }
    }

    /**
     * 停止 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     * @param reason          原因
     */
    private void stopWebsocketServer(Object commandReturner, String reason) {
        if (wsServer != null) {
            try {
                wsServer.stop(0, reason);
                Tool.commandReturn(commandReturner, reason);
            } catch (InterruptedException e) {
                Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                Tool.debugLog(e.getMessage());
            }
            wsServer = null;
        }
    }

    /**
     * 重载 WebSocket 服务器
     * 目前只有通过reload命令调用重载
     *
     * @param commandReturner 命令执行者
     */
    public void restartWebsocketServer(Object commandReturner) {
        stopWebsocketServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        startWebsocketServer(commandReturner);
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.RELOADED);
    }

    /**
     * 重载 Websocket
     * reload 命令调用
     *
     * @param commandReturner 命令执行者
     */
    public void restartWebsocket(Object commandReturner) {
        restartWebsocketServer(commandReturner);
        restartWebsocketClients(commandReturner);
    }

    /**
     * 启动 WebSocket
     * 开服时调用
     *
     * @param commandReturner 命令执行者
     */
    public void startWebsocket(Object commandReturner) {
        startWebsocketClients(commandReturner);
        startWebsocketServer(commandReturner);
    }

    /**
     * 因 Minecraft Server 开启，启动 WebSocket
     * 开服时调用
     */
    public void startWebsocketOnServerStart() {
        startWebsocket(null);
    }

    /**
     * 因 Minecraft Server 关闭，关闭 WebSocket
     * 关服时调用
     */
    public void stopWebsocketByServerClose() {
        stopWebsocket(1000, WebsocketConstantMessage.Client.CLOSING_CONNECTION, null);
    }

    /**
     * 停止 WebSocket
     * 除传入关闭码、关闭原因外，还需传入命令执行者（可为null）
     *
     * @param code            Code
     * @param reason          原因
     * @param commandReturner 命令执行者
     */
    public void stopWebsocket(int code, String reason, Object commandReturner) {
        stopWebsocketClients(code, reason, commandReturner);
        stopWebsocketServer(commandReturner, reason);
    }

    /**
     * 发送消息
     * 同时向所有 Websocket 客户端和服务端广播消息
     *
     * @param event 任何继承于 BaseEvent 的事件
     */
    public void sendEvent(BaseEvent event) {
        if (GlobalContext.getConfig().isEnable()) {
            String json = gson.toJson(event);
            wsClientList.forEach(wsClient -> wsClient.send(json));
            if (wsServer != null)
                wsServer.broadcast(json);
        }
    }
}
