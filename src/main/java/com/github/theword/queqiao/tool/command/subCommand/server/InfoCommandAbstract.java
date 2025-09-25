package com.github.theword.queqiao.tool.command.subCommand.server;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract;
import com.github.theword.queqiao.tool.command.subCommand.ServerCommandAbstract;
import com.github.theword.queqiao.tool.utils.Tool;
import com.github.theword.queqiao.tool.websocket.WsServer;
import org.java_websocket.WebSocket;

public abstract class InfoCommandAbstract extends ServerCommandAbstract {

    /**
     * 获取命令名称
     *
     * @return info
     */
    @Override
    public String getName() {
        return "info";
    }

    /**
     * 获取命令描述
     *
     * @return 获取 Websocket Server 信息
     */
    @Override
    public String getDescription() {
        return "获取 Websocket Server 信息";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link ServerCommandAbstract#getUsage()} info
     */
    @Override
    public String getUsage() {
        return super.getUsage() + " info";
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link ClientCommandAbstract#getPermissionNode()}.info
     */
    @Override
    public String getPermissionNode() {
        return super.getPermissionNode() + ".info";
    }

    /**
     * 获取 WebSocket 服务端状态 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     */
    @Override
    public void execute(Object commandReturner) {
        if (!GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            Tool.commandReturn(
                    commandReturner, "Websocket Server 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketServer 配置项");
            Tool.commandReturn(
                    commandReturner, String.format(
                            "配置项中地址为 %s:%d", GlobalContext.getConfig().getWebsocketServer().getHost(), GlobalContext.getConfig().getWebsocketServer().getPort()));
            return;
        }

        WsServer wsServer = GlobalContext.getWebsocketManager().getWsServer();

        if (wsServer == null) {
            Tool.commandReturn(commandReturner, "Websocket Server 为null，查询失败");
            return;
        }

        Tool.commandReturn(
                commandReturner, String.format(
                        "当前 Websocket Server 已开启，监听地址为 %s:%d", wsServer.getAddress().getHostString(), wsServer.getPort()));

        if (wsServer.getConnections().isEmpty()) {
            Tool.commandReturn(commandReturner, "当前暂无 Websocket 连接到该 Server");
            return;
        }

        Tool.commandReturn(
                commandReturner, String.format("当前 Websocket Server 已有 %d 个连接", wsServer.getConnections().size()));

        int count = 0;
        for (WebSocket webSocket : wsServer.getConnections()) {
            count++;
            Tool.commandReturn(
                    commandReturner, String.format(
                            "%d 来自 %s:%d 的连接", count, webSocket.getRemoteSocketAddress().getHostString(), webSocket.getRemoteSocketAddress().getPort()));
        }
    }

    /**
     * 占位
     *
     * <p>Pass
     *
     * @param commandReturner 命令执行者
     * @param boolVar         布尔值占位符
     */
    @Override
    public void execute(Object commandReturner, boolean boolVar) {
        execute(commandReturner);
    }
}
