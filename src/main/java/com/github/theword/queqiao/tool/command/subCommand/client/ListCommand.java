package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.utils.Tool;
import com.github.theword.queqiao.tool.websocket.WsClient;

import java.util.ArrayList;
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
     * <p>两处修正：
     * <ol>
     *     <li>配置的 URL 列表<b>只读取一次</b>——此前在循环条件里反复调用
     *         {@code getConfig().getWebsocketClient().getUrlList()}（每轮两次），
     *         并发 reload 时甚至可能中途换掉列表</li>
     *     <li>编号<b>统一从 1 开始</b>——此前"未启用"分支用 {@code i + 1}（从 1 起），
     *         而"已启用"分支用 {@code i}（从 0 起），同一条命令的编号规则随配置变化</li>
     * </ol>
     *
     * @param commandReturner 命令执行者
     * @param args            命令参数
     * @since 0.1.5
     */
    @Override
    protected void onExecute(Object commandReturner, List<String> args) {
        HandleCommandReturnMessageService returnMessageService = GlobalContext.getHandleCommandReturnMessageService();
        Config config = GlobalContext.getConfig();

        if (!config.get(ConfigKeys.WebSocketClient.ENABLE)) {
            List<String> urlList = new ArrayList<>(config.get(ConfigKeys.WebSocketClient.URL_LIST));
            returnMessageService.sendReturnMessage(
                    commandReturner, "Websocket Client 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketClient 配置项");
            returnMessageService.sendReturnMessage(
                    commandReturner, Tool.format("配置文件中连接列表如下共 {} 个 Client", urlList.size()));
            for (int i = 0; i < urlList.size(); i++) {
                returnMessageService.sendReturnMessage(
                        commandReturner, Tool.format("{} 连接至 {}", i + 1, urlList.get(i)));
            }
            return;
        }

        List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

        returnMessageService.sendReturnMessage(
                commandReturner, Tool.format("Websocket Client 列表，共 {} 个 Client", wsClientList.size()));

        for (int i = 0; i < wsClientList.size(); i++) {
            WsClient wsClient = wsClientList.get(i);
            returnMessageService.sendReturnMessage(
                    commandReturner,
                    Tool.format(
                            "{} 连接至 {} 的 Client，状态：{}",
                            i + 1,
                            wsClient.getURI(),
                            wsClient.isOpen() ? "已连接" : "未连接"));
        }
    }
}
