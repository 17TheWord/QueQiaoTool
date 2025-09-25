package com.github.theword.queqiao.tool.command.subCommand.client;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.command.subCommand.ClientCommandAbstract;
import com.github.theword.queqiao.tool.utils.Tool;
import com.github.theword.queqiao.tool.websocket.WsClient;
import java.util.List;

public abstract class ListCommandAbstract extends ClientCommandAbstract {
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
   * 获取命令前缀
   *
   * <p>用于遍历时判断前驱后继
   *
   * <p>前缀为命令头则代表根命令
   *
   * @return client
   */
  @Override
  public String getPrefix() {
    return "client";
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
   * 获取命令用法
   *
   * @return 命令用法 使用：/{@link ClientCommandAbstract#getUsage()} list
   */
  @Override
  public String getUsage() {
    return super.getUsage() + " list";
  }

  /**
   * 获取命令权限节点
   *
   * @return 权限节点 {@link ClientCommandAbstract#getPermissionNode()}.list
   */
  @Override
  public String getPermissionNode() {
    return super.getPermissionNode() + ".list";
  }

  /**
   * 获取 WebSocket 客户端状态 整合游戏内命令调用
   *
   * @param commandReturner 命令执行者
   * @since 0.1.5
   */
  @Override
  public void execute(Object commandReturner) {
    if (!GlobalContext.getConfig().getWebsocketClient().isEnable()) {
      Tool.commandReturn(
          commandReturner, "Websocket Client 配置项未启用，如需开启，请在 config.yml 中启用 WebsocketClient 配置项");
      Tool.commandReturn(
          commandReturner,
          "配置文件中连接列表如下共 "
              + GlobalContext.getConfig().getWebsocketClient().getUrlList().size()
              + " 个 Client");
      for (int i = 0; i < GlobalContext.getConfig().getWebsocketClient().getUrlList().size(); i++) {
        Tool.commandReturn(
            commandReturner,
            String.format(
                "%d 连接至 %s",
                i + 1, GlobalContext.getConfig().getWebsocketClient().getUrlList().get(i)));
      }
      return;
    }

    List<WsClient> wsClientList = GlobalContext.getWebsocketManager().getWsClientList();

    Tool.commandReturn(
        commandReturner, "Websocket Client 列表，共 " + wsClientList.size() + " 个 Client");

    for (int i = 0; i < wsClientList.size(); i++) {
      WsClient wsClient = wsClientList.get(i);
      Tool.commandReturn(
          commandReturner,
          String.format(
              "%d 连接至 %s 的 Client，状态：%s", i, wsClient.getURI(), wsClient.isOpen() ? "已连接" : "未连接"));
    }
  }

  /**
   * 获取 WebSocket 客户端状态
   *
   * <p>Pass
   *
   * @param commandReturner 命令执行者
   * @param boolVar 布尔值占位符
   * @since 0.1.5
   */
  @Override
  public void execute(Object commandReturner, boolean boolVar) {
    execute(commandReturner);
  }
}
