package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.CommandConstantMessage;
import com.github.theword.queqiao.tool.websocket.WsClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ReconnectCommand extends SubCommand {

    public ReconnectCommand() {
        addChild(new ReconnectAllCommand());
    }

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
        return "重新连接断开的 Websocket Clients";
    }

    /**
     * 获取命令用法（添加参数说明）
     *
     * @return 使用说明
     */
    @Override
    public String getUsage() {
        return getFullPath();
    }

    /**
     * 重连 WebSocket 客户端 reconnect 命令调用
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     */
    @Override
    public void execute(Object commandReturner, List<String> args) {
        reconnect(commandReturner, false);
    }

    public static void reconnect(Object commandReturner, boolean all) {
        String reconnectCount = all ? CommandConstantMessage.RECONNECT_ALL_CLIENT : CommandConstantMessage.RECONNECT_NOT_OPEN_CLIENT;
        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, reconnectCount);

        AtomicInteger opened = new AtomicInteger();

        List<WsClient> wsClientList = getClientList(commandReturner, all, opened);
        if (opened.get() == wsClientList.size()) {
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                    commandReturner, CommandConstantMessage.RECONNECT_NO_CLIENT_NEED_RECONNECT);
        }
        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(commandReturner, CommandConstantMessage.RECONNECTED);
    }

    private static List<WsClient> getClientList(Object commandReturner, boolean all, AtomicInteger opened) {
        List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

        wsClientList.forEach(
                wsClient -> {
                    if (all || !wsClient.isOpen()) {
                        wsClient.reconnectNow();
                        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                                commandReturner, String.format(CommandConstantMessage.RECONNECT_MESSAGE, wsClient.getURI()));
                    } else {
                        opened.getAndIncrement();
                    }
                });
        return wsClientList;
    }
}

