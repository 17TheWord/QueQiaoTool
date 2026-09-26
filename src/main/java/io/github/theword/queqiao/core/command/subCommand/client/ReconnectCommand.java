package io.github.theword.queqiao.core.command.subCommand.client;

import io.github.theword.queqiao.core.GlobalContext;
import io.github.theword.queqiao.core.command.SubCommand;
import io.github.theword.queqiao.core.constant.CommandConstant;
import io.github.theword.queqiao.core.handle.HandleCommandReturnMessageService;
import io.github.theword.queqiao.core.utils.Tool;
import io.github.theword.queqiao.core.websocket.WsClient;

import java.util.List;

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
    protected void onExecute(Object commandReturner, List<String> args) {
        reconnect(commandReturner, false);
    }

    /**
     * 重连 WebSocket 客户端
     *
     * <p><b>{@code all} 的语义</b>：
     * <ul>
     *     <li>{@code false} —— 只重连<b>未打开</b>的客户端（{@code /queqiao client reconnect}）</li>
     *     <li>{@code true} —— <b>强制</b>重连全部客户端，含当前健康的连接
     *         （{@code /queqiao client reconnect all}），会主动断开再重建</li>
     * </ul>
     *
     * <p><b>注意</b>：本方法只是把重连任务<b>投递到调度器</b>，并不保证连接成功，
     * 因此结束提示为"已安排重连"而非"已重新连接"；真正的结果由
     * {@code WsClient} 在 {@code onOpen} / {@code onClose} 中记录到日志。
     *
     * @param commandReturner 命令执行者
     * @param all             是否强制重连全部客户端
     */
    public static void reconnect(Object commandReturner, boolean all) {
        HandleCommandReturnMessageService returnMessageService = GlobalContext.getHandleCommandReturnMessageService();
        returnMessageService.sendReturnMessage(
                commandReturner, all ? CommandConstant.RECONNECT_ALL_CLIENT : CommandConstant.RECONNECT_NOT_OPEN_CLIENT);

        List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

        int alreadyOpenCount = 0;
        for (WsClient wsClient : wsClientList) {
            if (!all && wsClient.isOpen()) {
                alreadyOpenCount++;
                continue;
            }
            wsClient.reconnectNow();
            returnMessageService.sendReturnMessage(
                    commandReturner, Tool.format(CommandConstant.RECONNECT_MESSAGE, wsClient.getURI()));
        }

        if (alreadyOpenCount == wsClientList.size()) {
            returnMessageService.sendReturnMessage(commandReturner, CommandConstant.RECONNECT_NO_CLIENT_NEED_RECONNECT);
        }
        returnMessageService.sendReturnMessage(commandReturner, CommandConstant.RECONNECTED);
    }
}
