package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.websocket.WsClient;
import java.util.List;

public class ListCommand extends SubCommand {
    /**
     * 获取命令名称
     *
     * @return list
     */
    @Override
    public String getName() {
        return "list";
    }

    /**
     * 获取命令描述
     *
     * @return 获取当前 Websocket Client 列表
     */
    @Override
    public String getDescription() {
        return "获取当前 Websocket Client 列表";
    }

    /**
     * 获取 WebSocket 客户端状态 整合游戏内命令调用
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     * @since 0.1.5
     */
    @Override
    public void execute(Object commandReturner, List<String> args) {
        if (!GlobalContext.getConfig().getWebsocketClient().isEnable()) {
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                    commandReturner, "Websocket Client 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketClient 配置项");
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                    commandReturner, "配置文件中连接列表如下共 " + GlobalContext.getConfig().getWebsocketClient().getUrlList().size() + " 个 Client");
            for (int i = 0; i < GlobalContext.getConfig().getWebsocketClient().getUrlList().size(); i++) {
                GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                        commandReturner, String.format(
                                "%d 连接至 %s", i + 1, GlobalContext.getConfig().getWebsocketClient().getUrlList().get(i)));
            }
            return;
        }

        List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

        GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                commandReturner, "Websocket Client 列表，共 " + wsClientList.size() + " 个 Client");

        for (int i = 0; i < wsClientList.size(); i++) {
            WsClient wsClient = wsClientList.get(i);
            GlobalContext.getHandleCommandReturnMessageService().sendReturnMessage(
                    commandReturner, String.format(
                            "%d 连接至 %s 的 Client，状态：%s", i, wsClient.getURI(), wsClient.isOpen() ? "已连接" : "未连接"));
        }
    }
}
