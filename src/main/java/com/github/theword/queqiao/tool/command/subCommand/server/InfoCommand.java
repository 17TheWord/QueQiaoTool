package com.github.theword.queqiao.tool.command.subCommand.server;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.websocket.WsServer;
import java.net.InetSocketAddress;
import java.util.List;

public class InfoCommand extends SubCommand {

    /**
     * 获取命令名称。
     *
     * @return info
     */
    @Override
    public String getName() {
        return "info";
    }

    /**
     * 获取命令描述。
     *
     * @return 获取 Websocket Server 信息
     */
    @Override
    public String getDescription() {
        return "获取 Websocket Server 信息";
    }

    /**
     * 获取 WebSocket 服务端状态。
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        HandleCommandReturnMessageService returnMessageService = GlobalContext.getHandleCommandReturnMessageService();
        if (!GlobalContext.getConfig().getWebsocketServer().isEnable()) {
            returnMessageService.sendReturnMessage(
                    commandReturner, "Websocket Server 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketServer 配置项");
            returnMessageService.sendReturnMessage(
                    commandReturner, String.format(
                            "配置项中地址为 %s:%d", GlobalContext.getConfig().getWebsocketServer().getHost(), GlobalContext.getConfig().getWebsocketServer().getPort()));
            return;
        }

        WsServer wsServer = GlobalContext.getWebsocketManager().getWsServer();
        if (wsServer == null) {
            returnMessageService.sendReturnMessage(commandReturner, "Websocket Server 为 null，查询失败");
            return;
        }

        returnMessageService.sendReturnMessage(
                commandReturner, String.format(
                        "当前 Websocket Server 已开启，监听地址为 %s:%d", wsServer.getAddress().getHostString(), wsServer.getPort()));

        List<InetSocketAddress> connections = wsServer.getConnections();
        if (connections.isEmpty()) {
            returnMessageService.sendReturnMessage(commandReturner, "当前暂无 Websocket 连接到该 Server");
            return;
        }

        returnMessageService.sendReturnMessage(
                commandReturner, String.format("当前 Websocket Server 已有 %d 个连接", connections.size()));

        int count = 1;
        for (InetSocketAddress address : connections) {
            returnMessageService.sendReturnMessage(
                    commandReturner, String.format("%d 来自 %s:%d 的连接", count++, address.getHostString(), address.getPort()));
        }
    }
}
