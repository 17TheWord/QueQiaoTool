package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract;
import com.github.theword.queqiao.tool.constant.CommandConstantMessage;
import com.github.theword.queqiao.tool.utils.Tool;
import com.github.theword.queqiao.tool.websocket.WsClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class ReconnectCommandAbstract extends ClientCommandAbstract {

    /**
     * 获取命令名称
     *
     * @return reconnect
     */
    @Override
    public String getName() {
        return "reconnect";
    }

    /**
     * 获取命令描述
     *
     * @return 重新连接 Websocket Clients
     */
    @Override
    public String getDescription() {
        return "重新连接 Websocket Clients";
    }

    /**
     * 获取命令用法
     *
     * @return 使用：/{@link ClientCommandAbstract#getUsage()} reconnect [all]
     */
    @Override
    public String getUsage() {
        return super.getUsage() + " reconnect [all]";
    }

    /**
     * 获取命令权限节点
     *
     * @return {@link ClientCommandAbstract#getPermissionNode()}.reconnect
     */
    @Override
    public String getPermissionNode() {
        return super.getPermissionNode() + ".reconnect";
    }

    /**
     * 执行命令
     * <p>Pass</p>
     *
     * @param commandReturner 命令执行者
     */
    @Override
    public void execute(Object commandReturner) {
        execute(commandReturner, false);
    }

    /**
     * 重连 WebSocket 客户端
     * reconnect [all] 命令调用
     *
     * @param all             是否全部重连
     * @param commandReturner 命令执行者
     */
    @Override
    public void execute(Object commandReturner, boolean all) {
        String reconnectCount = all ? CommandConstantMessage.RECONNECT_ALL_CLIENT : CommandConstantMessage.RECONNECT_NOT_OPEN_CLIENT;
        Tool.commandReturn(commandReturner, reconnectCount);

        AtomicInteger opened = new AtomicInteger();

        List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

        wsClientList.forEach(wsClient -> {
            if (all || !wsClient.isOpen()) {
                wsClient.reconnectNow();
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
}
