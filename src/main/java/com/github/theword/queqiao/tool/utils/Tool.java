package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.constant.BaseConstant;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.handle.HandleApi;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessage;
import com.github.theword.queqiao.tool.payload.modle.CommonBaseComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工具类
 */
public class Tool {
    /**
     * 日志
     */
    public static Logger logger = null;
    /**
     * 配置项
     */
    public static Config config = null;
    /**
     * Websocket 管理器
     */
    public static WebsocketManager websocketManager = null;
    /**
     * api 消息处理
     */
    public static HandleApi handleApi = null;
    /**
     * 命令消息处理
     */
    public static HandleCommandReturnMessage handleCommandReturnMessage = null;

    /**
     * 组件初始化，以组件名作为日志名；
     * 根据服务端类型读取配置文件与初始化配置项；
     * 初始化 Websocket 管理器；
     * 初始化 api 消息处理与命令消息处理；
     * 初始化完成后，打印初始化完成日志
     *
     * @param isModServer                       是否为模组服务器
     * @param handleApiService                  api消息处理
     * @param handleCommandReturnMessageService 命令消息处理
     */
    public static void initTool(boolean isModServer, HandleApi handleApiService, HandleCommandReturnMessage handleCommandReturnMessageService) {
        logger = LoggerFactory.getLogger(BaseConstant.MODULE_NAME);
        logger.info(BaseConstant.LAUNCHING);
        config = Config.loadConfig(isModServer);
        websocketManager = new WebsocketManager();
        handleApi = handleApiService;
        handleCommandReturnMessage = handleCommandReturnMessageService;
        logger.info(BaseConstant.INITIALIZED);
    }

    /**
     * 发送消息
     * 同时向所有 Websocket 客户端和服务端广播消息
     *
     * @param event 任何继承于 BaseEvent 的事件
     */
    public static void sendWebsocketMessage(BaseEvent event) {
        if (config.isEnable()) {
            event.setServerName(config.getServer_name());
            websocketManager.getWsClientList().forEach(wsClient -> wsClient.send(event.getJson()));
            if (websocketManager.getWsServer() != null)
                websocketManager.getWsServer().broadcast(event.getJson());
        }
    }


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
     * 命令返回
     * 当有执行者时，向执行者返回相应的消息作回执
     *
     * @param commandReturner 命令返回
     * @param message         消息
     */
    public static void commandReturn(Object commandReturner, String message) {
        if (commandReturner != null)
            handleCommandReturnMessage.handleCommandReturnMessage(commandReturner, message);
    }

    /**
     * DEBUG模式
     * 用于输出更多内容
     *
     * @param message 消息
     */
    public static void debugLog(String message) {
        if (config.isDebug())
            logger.info(message);
    }

    /**
     * DEBUG模式
     * 用于输出更多内容
     *
     * @param format 格式
     * @param args   参数
     */
    public static void debugLog(String format, Object... args) {
        if (config.isDebug()) {
            logger.debug(format, args);
        }
    }

    /**
     * 获取发送消息的前缀字符
     * 可通过配置文件自定义
     *
     * @return 前缀
     */
    public static CommonBaseComponent getPrefixComponent() {
        CommonBaseComponent commonBaseComponent = new CommonBaseComponent();
        commonBaseComponent.setText("[" + config.getMessage_prefix() + "] ");
        commonBaseComponent.setColor("gold");
        return commonBaseComponent;
    }
}
