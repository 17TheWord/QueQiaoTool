package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.constant.CommandConstantMessage;
import com.github.theword.queqiao.tool.constant.WebsocketConstantMessage;
import com.github.theword.queqiao.tool.websocket.WsClient;
import com.github.theword.queqiao.tool.websocket.WsServer;
import lombok.Getter;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.theword.queqiao.tool.utils.Tool.config;

@Getter
public class WebsocketManager {
    /**
     * Websocket Client 列表
     */
    private final List<WsClient> wsClientList = new ArrayList<>();
    /**
     * Websocket Server
     */
    private WsServer wsServer;

    /**
     * 启动 WebSocket 客户端
     * 需传入可为null的命令执行者
     *
     * @param commandReturner 命令执行者
     */
    private void startWebsocketClients(Object commandReturner) {
        if (config.getWebsocketClient().isEnable()) {
            Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.LAUNCHING);
            config.getWebsocketClient().getUrlList().forEach(websocketUrl -> {
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
        wsClientList.forEach(wsClient -> {
            Tool.commandReturn(commandReturner, String.format(reason, wsClient.getURI()));
            wsClient.stopWithoutReconnect(code, String.format(reason, wsClient.getURI()));
        });
        wsClientList.clear();
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Client.CLEAR_WEBSOCKET_CLIENT_LIST);
    }


    /**
     * 重载 WebSocket 客户端
     *
     * @param commandReturner 命令执行者
     */
    private void restartWebsocketClients(Object commandReturner) {
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
        if (config.getWebsocketServer().isEnable()) {
            wsServer = new WsServer(new InetSocketAddress(config.getWebsocketServer().getHost(), config.getWebsocketServer().getPort()));
            wsServer.start();
            Tool.commandReturn(commandReturner, String.format(WebsocketConstantMessage.Server.SERVER_STARTING, config.getWebsocketServer().getHost(), config.getWebsocketServer().getPort()));
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
    private void restartWebsocketServer(Object commandReturner) {
        stopWebsocketServer(commandReturner, WebsocketConstantMessage.Server.RELOADING);
        startWebsocketServer(commandReturner);
        Tool.commandReturn(commandReturner, WebsocketConstantMessage.Server.RELOADED);
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
        Tool.commandReturn(commandReturner, CommandConstantMessage.RELOAD_CONFIG);
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

        AtomicInteger opened = new AtomicInteger();
        wsClientList.forEach(wsClient -> {
            if (all || !wsClient.isOpen()) {
                wsClient.reconnectWebsocket();
                Tool.commandReturn(commandReturner, String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
            } else {
                opened.getAndIncrement();
            }
        });
        if (opened.get() == wsClientList.size()) {
            Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
        }
        Tool.commandReturn(commandReturner, CommandConstantMessage.RECONNECTED);
    }


    /**
     * 获取 WebSocket 服务端状态
     * 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     */
    public void getWebsocketServerStatus(Object commandReturner) {
        if (!config.getWebsocketServer().isEnable()) {
            Tool.commandReturn(commandReturner, "Websocket Server 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketServer 配置项");
            Tool.commandReturn(commandReturner, String.format("配置项中地址为 %s:%d", config.getWebsocketServer().getHost(), config.getWebsocketServer().getPort()));
            return;
        }

        if (wsServer == null) {
            Tool.commandReturn(commandReturner, "Websocket Server 为null，查询失败");
            return;
        }

        Tool.commandReturn(commandReturner, String.format("当前 Websocket Server 已开启，监听地址为 %s:%d", wsServer.getAddress().getHostString(), wsServer.getPort()));

        if (wsServer.getConnections().isEmpty()) {
            Tool.commandReturn(commandReturner, "当前暂无 Websocket 连接到该 Server");
            return;
        }

        Tool.commandReturn(commandReturner, String.format("当前 Websocket Server 已有 %d 个连接", wsServer.getConnections().size()));

        int count = 0;
        for (WebSocket webSocket : wsServer.getConnections()) {
            count++;
            Tool.commandReturn(commandReturner, String.format("%d 来自 %s:%d 的连接", count, webSocket.getRemoteSocketAddress().getHostString(), webSocket.getRemoteSocketAddress().getPort()));
        }
    }

    /**
     * 获取 WebSocket 客户端状态
     * 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     */
    public void getWebsocketClientStatus(Object commandReturner) {
        if (!config.getWebsocketClient().isEnable()) {
            Tool.commandReturn(commandReturner, "Websocket Client 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketClient 配置项");
            Tool.commandReturn(commandReturner, "配置文件中连接列表如下共 " + config.getWebsocketClient().getUrlList().size() + " 个 Client");
            for (int i = 0; i < config.getWebsocketClient().getUrlList().size(); i++) {
                Tool.commandReturn(commandReturner, String.format("%d 连接至 %s", i + 1, config.getWebsocketClient().getUrlList().get(i)));
            }
            return;
        }

        Tool.commandReturn(commandReturner, "Websocket Client 列表，共 " + wsClientList.size() + " 个 Client");

        for (int i = 0; i < wsClientList.size(); i++) {
            WsClient wsClient = wsClientList.get(i);
            Tool.commandReturn(commandReturner, String.format("%d 连接至 %s 的 Client，状态：%s", i, wsClient.getURI(), wsClient.isOpen() ? "已连接" : "未连接"));
        }
    }
}
