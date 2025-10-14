package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.google.gson.JsonObject;

/**
 * 工具类
 *
 * <p>持有全局静态对象与公共工具方法，例如日志、配置、WebSocket 管理等。
 */
public class Tool {

    /**
     * 判断是否为注册或登录命令
     *
     * @param command 命令
     * @return 是否为注册或登录命令，如果是，返回空字符串
     */
    public static String isRegisterOrLoginCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        if (command.startsWith("l ") || command.startsWith("login ") || command.startsWith("register ") || command.startsWith("reg ") || command.startsWith(BaseConstant.COMMAND_HEADER + " "))
            return "";
        return command;
    }

    /**
     * DEBUG模式 用于输出更多内容
     *
     * @param message 消息
     */
    public static void debugLog(String message) {
        if (GlobalContext.getConfig().isDebug()) {
            GlobalContext.getLogger().info(message);
        }
    }

    /**
     * DEBUG模式 用于输出更多内容
     *
     * @param format 格式
     * @param args   参数
     */
    public static void debugLog(String format, Object... args) {
        if (GlobalContext.getConfig().isDebug()) {
            GlobalContext.getLogger().info(format, args);
        }
    }

    /**
     * 获取发送消息的前缀字符
     *
     * <p>可通过配置文件自定义
     *
     * <p>默认为：[鹊桥]
     *
     * @return 前缀
     */
    public static JsonObject getPrefixComponent() {
        JsonObject prefixJsonElement = new JsonObject();
        prefixJsonElement.addProperty("text", GlobalContext.getConfig().getMessagePrefix());
        prefixJsonElement.addProperty("color", "yellow");
        prefixJsonElement.addProperty("bold", false);
        return prefixJsonElement;
    }
}
