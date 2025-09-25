package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public class ClientCommandAbstract implements SubCommand {

  /**
   * 获取命令名称
   *
   * @return client
   */
  @Override
  public String getName() {
    return "client";
  }

  /**
   * 获取命令前缀
   *
   * <p>用于遍历时判断前驱后继
   *
   * <p>为空字符串则代表根命令
   *
   * @return {@link BaseConstant#COMMAND_HEADER}
   */
  @Override
  public String getPrefix() {
    return BaseConstant.COMMAND_HEADER;
  }

  /**
   * 获取命令描述
   *
   * @return Websocket Client 命令
   */
  @Override
  public String getDescription() {
    return "Websocket Client 命令";
  }

  /**
   * 获取命令用法
   *
   * @return 使用：/{@link BaseConstant#COMMAND_HEADER} client
   */
  @Override
  public String getUsage() {
    return "使用：/" + BaseConstant.COMMAND_HEADER + " client";
  }

  /**
   * 获取命令权限节点
   *
   * @return {@link BaseConstant#COMMAND_HEADER}.client
   */
  @Override
  public String getPermissionNode() {
    return BaseConstant.COMMAND_HEADER + ".client";
  }

  /**
   * 执行命令
   *
   * <p>Pass
   *
   * @param commandReturner 命令执行者
   * @param boolVar 布尔值占位符
   */
  @Override
  public void execute(Object commandReturner, boolean boolVar) {
    // pass
  }

  /**
   * 执行命令
   *
   * <p>Pass
   *
   * @param commandReturner 命令执行者
   */
  @Override
  public void execute(Object commandReturner) {
    // pass
  }
}
