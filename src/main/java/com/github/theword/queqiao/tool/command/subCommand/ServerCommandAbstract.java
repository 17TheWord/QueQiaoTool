package com.github.theword.queqiao.tool.command.subCommand;

import com.github.theword.queqiao.tool.command.SubCommand;
import com.github.theword.queqiao.tool.constant.BaseConstant;

public class ServerCommandAbstract implements SubCommand {

  /**
   * 获取命令名称
   *
   * @return server
   */
  @Override
  public String getName() {
    return "server";
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
   * @return Websocket Server 命令
   */
  @Override
  public String getDescription() {
    return "Websocket Server 命令";
  }

  /**
   * 获取命令用法
   *
   * @return 使用：/{@link BaseConstant#COMMAND_HEADER} server
   */
  @Override
  public String getUsage() {
    return "使用：/" + BaseConstant.COMMAND_HEADER + " server";
  }

  /**
   * 获取命令权限节点
   *
   * @return {@link BaseConstant#COMMAND_HEADER}.server
   */
  @Override
  public String getPermissionNode() {
    return BaseConstant.COMMAND_HEADER + ".server";
  }

  /**
   * 执行命令
   *
   * <p>位于本命令 pass
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
   * <p>位于本命令 pass
   *
   * @param commandReturner 命令执行者
   */
  @Override
  public void execute(Object commandReturner) {
    // pass
  }
}
