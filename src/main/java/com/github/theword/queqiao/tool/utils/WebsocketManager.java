package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.constant.CommandConstantMessage;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.websocket.WsClient;
import com.github.theword.queqiao.tool.websocket.WsServer;
import lombok.Getter;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class WebsocketManager {
    /**
     * Websocket Server
     */
    private WsServer wsServer;
    /**
     * Websocket Client 列表
     */
    private final List<WsClient> wsClientList = new ArrayList<>();

    /**
     * 启动 WebSocket 客户端
     * 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketClients(Object commandReturner) {
        if (Tool.config.getWebsocket_client().isEnable()) {
            Tool.logger.info(WebsocketConstantMessage.Client.LAUNCHING);
            Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
            Tool.config.getWebsocket_client().getUrl_list().forEach(websocketUrl -> {
                try {
                    WsClient wsClient = new WsClient(new URI(websocketUrl));
                    wsClient.connect();
                    wsClientList.add(wsClient);
                } catch (URISyntaxException e) {
                    Tool.commandReturn(commandReturner, String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl));
                    Tool.logger.warn(String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl));
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
        wsClientList.forEach(wsClient -> {
            Tool.commandReturn(commandReturner, String.format(reason, wsClient.getURI()));
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.getURI()));
            Tool.logger.info(String.format(reason, wsClient.getURI()));
        });
        wsClientList.clear();
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
        Tool.logger.info(WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }


    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    private void restartWebsocketClients(Object commandReturner) {
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        Tool.logger.info(WebsocketConstantMessage.Client.RELOADING);
        stopWebsocketClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        startWebsocketClients(commandReturner);
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADED);
        Tool.logger.info(WebsocketConstantMessage.Client.RELOADED);
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketServer(Object commandReturner) {
        if (Tool.config.getWebsocket_server().isEnable()) {
            wsServer = new WsServer(new InetSocketAddress(Tool.config.getWebsocket_server().getHost(), Tool.config.getWebsocket_server().getPort()));
            wsServer.start();
            Tool.commandReturn(commandReturner, String.format(WebsocketConstantMessage.Server.SERVER_STARTING, Tool.config.getWebsocket_server().getHost(), Tool.config.getWebsocket_server().getPort()));
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
                Tool.logger.info(reason);
            } catch (InterruptedException e) {
                Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                Tool.logger.warn(WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
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
    private void restartWebsocketServer(Object commandReturner) {
        stopWebsocketServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        startWebsocketServer(commandReturner);
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.RELOADED);
        Tool.logger.info(WebsocketConstantMessage.Server.RELOADED);
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
     * 重载 WebSocket
     * reload 命令调用
     *
     * @param isModServer     是否为 ModServer
     * @param commandReturner 命令执行者
     */
    public void reloadWebsocket(boolean isModServer, Object commandReturner) {
        Tool.config = Config.loadConfig(isModServer);
        Tool.commandReturn(commandReturner, CommandConstantMessage.RELOAD_CONFIG);
        Tool.logger.info(CommandConstantMessage.RELOAD_CONFIG);
        restartWebsocketServer(commandReturner);
        restartWebsocketClients(commandReturner);
    }

    /**
     * 重连 WebSocket 客户端
     * reconnect [all] 命令调用
     *
     * @param all             是否全部重连
     * @param commandReturner 命令执行者
     */
    public void reconnectWebsocketClients(boolean all, Object commandReturner) {
        String reconnectCount = all ? CommandConstantMessage.RECONNECT_ALL_CLIENT : CommandConstantMessage.RECONNECT_NOT_OPEN_CLIENT;
        Tool.commandReturn(commandReturner, reconnectCount);
        Tool.logger.info(reconnectCount);

        AtomicInteger opened = new AtomicInteger();
        wsClientList.forEach(wsClient -> {
            if (all || !wsClient.isOpen()) {
                wsClient.reconnectWebsocket();
                Tool.commandReturn(commandReturner, String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
                Tool.logger.info(String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
            } else {
                opened.getAndIncrement();
            }
        });
        if (opened.get() == wsClientList.size()) {
            Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
            Tool.logger.info(CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
        }
        Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECTED);
        Tool.logger.info(CommandConstantMessage.RECONNECTED);
    }
}
