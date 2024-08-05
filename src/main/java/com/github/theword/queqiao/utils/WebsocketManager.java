package com.github.theword.queqiao.utils;

import com.github.theword.queqiao.constant.CommandConstantMessage;
import com.github.theword.queqiao.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.websocket.WsClient;
import com.github.theword.queqiao.websocket.WsServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.theword.queqiao.utils.Tool.*;

public class WebsocketManager {
    /**
     * 启动 WebSocket 客户端
     * 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketClients(Object commandReturner) {
        if (config.getWebsocket_client().isEnable()) {
            logger.info(WebsocketConstantMessage.Client.LAUNCHING);
            commandReturn(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
            config.getWebsocket_client().getUrl_list().forEach(websocketUrl -> {
                try {
                    WsClient wsClient = new WsClient(new URI(websocketUrl));
                    wsClient.connect();
                    wsClientList.add(wsClient);
                } catch (URISyntaxException e) {
                    commandReturn(commandReturner, String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl));
                    logger.warn(String.format(WebsocketConstantMessage.Client.URI_SYNTAX_ERROR, websocketUrl));
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
            commandReturn(commandReturner, String.format(reason, wsClient.getURI()));
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.getURI()));
            logger.info(String.format(reason, wsClient.getURI()));
        });
        wsClientList.clear();
        commandReturn(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
        logger.info(WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }


    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    private void restartWebsocketClients(Object commandReturner) {
        commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADING);
        logger.info(WebsocketConstantMessage.Client.RELOADING);
        stopWebsocketClients(1000, WebsocketConstantMessage.CLOSE_BY_RELOAD, commandReturner);
        startWebsocketClients(commandReturner);
        commandReturn(commandReturner, WebsocketConstantMessage.Client.RELOADED);
        logger.info(WebsocketConstantMessage.Client.RELOADED);
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketServer(Object commandReturner) {
        if (config.getWebsocket_server().isEnable()) {
            wsServer = new WsServer(new InetSocketAddress(config.getWebsocket_server().getHost(), config.getWebsocket_server().getPort()));
            wsServer.start();
            commandReturn(commandReturner, String.format(WebsocketConstantMessage.Server.SERVER_STARTING, config.getWebsocket_server().getHost(), config.getWebsocket_server().getPort()));
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
                commandReturn(commandReturner, reason);
                logger.info(reason);
            } catch (InterruptedException e) {
                commandReturn(commandReturner, WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                logger.warn(WebsocketConstantMessage.Server.ERROR_ON_STOPPING);
                debugLog(e.getMessage());
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
        commandReturn(commandReturner, WebsocketConstantMessage.Server.RELOADED);
        logger.info(WebsocketConstantMessage.Server.RELOADED);
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
        config = Config.loadConfig(isModServer);
        commandReturn(commandReturner, CommandConstantMessage.RELOAD_CONFIG);
        logger.info(CommandConstantMessage.RELOAD_CONFIG);
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
        commandReturn(commandReturner, reconnectCount);
        logger.info(reconnectCount);

        AtomicInteger opened = new AtomicInteger();
        wsClientList.forEach(wsClient -> {
            if (all || !wsClient.isOpen()) {
                wsClient.reconnectWebsocket();
                commandReturn(commandReturner, String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
                logger.info(String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
            } else {
                opened.getAndIncrement();
            }
        });
        if (opened.get() == wsClientList.size()) {
            commandReturn(commandReturner, CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
            logger.info(CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
        }
        commandReturn(commandReturner, CommandConstantMessage.RECONNECTED);
        logger.info(CommandConstantMessage.RECONNECTED);
    }
}
