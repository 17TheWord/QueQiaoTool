package io.github.theword.queqiao.core.command.subCommand.server;

import io.github.theword.queqiao.core.GlobalContext;
import io.github.theword.queqiao.core.command.SubCommand;
import io.github.theword.queqiao.core.config.ConfigKeys;
import io.github.theword.queqiao.core.config.Config;
import io.github.theword.queqiao.core.handle.HandleCommandReturnMessageService;
import io.github.theword.queqiao.core.utils.Tool;
import io.github.theword.queqiao.core.websocket.WsServer;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends SubCommand {

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
     * 获取 WebSocket 服务端状态 整合游戏内命令调用
     *
     * <p><b>连接集合只读取一次</b>：此前 {@code getConnections()} 被调用三次
     * （判空 / 取数量 / 遍历），三次之间集合可能变化，
     * 会出现"显示 N 个连接、却列出 M 条"的自相矛盾输出。
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        HandleCommandReturnMessageService returnMessageService = GlobalContext.getHandleCommandReturnMessageService();

        Config config = GlobalContext.getConfig();
        if (!config.get(ConfigKeys.WebSocket.ENABLE)) {
            returnMessageService.sendReturnMessage(
                    commandReturner, "Websocket Server 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketServer 配置项");
            returnMessageService.sendReturnMessage(
                    commandReturner, Tool.format("配置项中地址为 {}:{}", config.get(ConfigKeys.WebSocket.HOST), config.get(ConfigKeys.WebSocket.PORT)));
            return;
        }

        WsServer wsServer = GlobalContext.getWebsocketManager().getWsServer();
        if (wsServer == null) {
            returnMessageService.sendReturnMessage(commandReturner, "Websocket Server 为 null，查询失败");
            return;
        }

        returnMessageService.sendReturnMessage(
                commandReturner,
                Tool.format(
                        "当前 Websocket Server 已开启，监听地址为 {}:{}",
                        wsServer.getAddress().getHostString(),
                        wsServer.getPort()));

        // 取一次快照后统一使用，避免三次读取之间集合变化导致输出自相矛盾
        List<WebSocket> connections = new ArrayList<>(wsServer.getConnections());
        if (connections.isEmpty()) {
            returnMessageService.sendReturnMessage(commandReturner, "当前暂无 Websocket 连接到该 Server");
            return;
        }

        returnMessageService.sendReturnMessage(
                commandReturner, Tool.format("当前 Websocket Server 已有 {} 个连接", connections.size()));

        int count = 0;
        for (WebSocket webSocket : connections) {
            count++;
            InetSocketAddress remoteAddress = webSocket.getRemoteSocketAddress();
            if (remoteAddress == null) {
                returnMessageService.sendReturnMessage(commandReturner, Tool.format("{} 来自未知地址的连接", count));
            } else {
                returnMessageService.sendReturnMessage(
                        commandReturner,
                        Tool.format("{} 来自 {}:{} 的连接", count, remoteAddress.getHostString(), remoteAddress.getPort()));
            }
        }
    }
}
