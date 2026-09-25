package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.config.Config;
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
     * @deprecated 0.4.2，请使用 {@link #isIgnoredCommand(String)} 代替
     */
    public static String isRegisterOrLoginCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        if (command.startsWith("l ") || command.startsWith("login ") || command.startsWith("register ") || command.startsWith("reg ") || command.startsWith(BaseConstant.COMMAND_HEADER + " "))
            return "";
        return command;
    }

    /**
     * 判断是否为需要忽略的命令
     *
     * @param command 命令
     * @return 如果是，返回空字符串，否则返回原命令
     * @since 0.4.2
     */
    public static String isIgnoredCommand(String command) {
        if (command == null) return "";

        command = command.trim();
        if (command.isEmpty()) return "";

        if (command.startsWith("/")) command = command.substring(1);

        String commandHeader = command.split(" ", 2)[0].toLowerCase();
        if (ConfigKeys.effectiveIgnoredCommands(GlobalContext.getConfig()).contains(commandHeader)) return "";
        return command;
    }

    /**
     * DEBUG模式 用于输出更多内容
     *
     * @param message 消息
     */
    public static void debugLog(String message) {
        if (!isDebugEnabled()) {
            return;
        }
        GlobalContext.getLogger().info("[DEBUG] " + message);
    }

    /**
     * DEBUG模式 用于输出更多内容
     *
     * @param format 格式
     * @param args   参数
     */
    public static void debugLog(String format, Object... args) {
        if (!isDebugEnabled()) {
            return;
        }
        GlobalContext.getLogger().info("[DEBUG] " + format, args);
    }

    /**
     * 按 {@code {}} 占位符格式化消息
     *
     * <p><b>为什么需要它</b>：项目里的共享消息常量（{@code WebsocketConstantMessage}、
     * {@code CommandConstant}）使用 {@code {}} 占位符——与 SLF4J 日志风格一致。
     * 但同一个常量往往还要用于<b>非日志</b>场景（关闭原因、命令输出等），
     * 而 {@link String#format} 需要的是 {@code %s}：
     * 把 {@code {}} 常量交给它会导致占位符<b>原样输出</b>且参数被静默忽略。
     * （该问题真实发生过：关闭帧的原因里带着字面 {@code {}}。）
     *
     * <p><b>使用约定</b>：
     * <ul>
     *     <li>本项目自己的消息（共享常量或内联字符串）——一律用本方法，占位符写 {@code {}}</li>
     *     <li>来自 Minecraft 语言文件的翻译模板——必须用 {@link String#format}，
     *         因为这类模板使用 {@code %1$s} 位置参数，本方法不支持</li>
     * </ul>
     *
     * <p><b>行为</b>（与 SLF4J 的 {@code {}} 语义一致）：
     * 按出现顺序依次替换；参数不足时剩余占位符保持原样；参数多余时忽略；
     * 参数为 null 时输出 {@code "null"}；模板为 null 时返回 {@code "null"}。
     * 无参数或无占位符时直接返回原模板（零分配）。
     *
     * @param template 模板，允许为 null
     * @param args     替换参数
     * @return 替换后的文本
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return "null";
        }
        if (args == null || args.length == 0 || template.indexOf("{}") < 0) {
            return template;
        }

        StringBuilder builder = new StringBuilder(template.length() + 32);
        int cursor = 0;
        int argIndex = 0;
        while (argIndex < args.length) {
            int placeholder = template.indexOf("{}", cursor);
            if (placeholder < 0) {
                break;
            }
            builder.append(template, cursor, placeholder);
            builder.append(args[argIndex++]);
            cursor = placeholder + 2;
        }
        builder.append(template, cursor, template.length());
        return builder.toString();
    }

    /**
     * 判断是否可以输出调试日志
     *
     * <p>对 {@code GlobalContext} 尚未初始化的情况做空值防护：
     * 此前直接调用 {@code GlobalContext.getConfig().isDebug()}，
     * 在运行时空对象状态下会抛出 {@link NullPointerException}。
     *
     * <p>对外暴露是为了让调用方在构造昂贵的日志参数（如脱敏后的请求体）之前先判断，
     * 避免"日志关闭却仍付出构造代价"。
     *
     * @return true 表示配置已加载、日志实现可用且开启了 debug
     */
    public static boolean isDebugEnabled() {
        Config config = GlobalContext.getConfig();
        if (config == null || !config.get(ConfigKeys.DEBUG)) {
            return false;
        }
        return GlobalContext.getLogger() != null;
    }

    /**
     * 获取发送消息的前缀字符
     *
     * <p>可通过配置文件自定义
     *
     * <p>默认为：[鹊桥]
     *
     * @return 前缀
     * @deprecated 0.4.2，请使用 {@link GlobalContext#getMessagePrefixJsonObject()} 代替
     */
    public static JsonObject getPrefixComponent() {
        JsonObject prefixJsonElement = new JsonObject();
        prefixJsonElement.addProperty("text", GlobalContext.getConfig().get(ConfigKeys.MESSAGE_PREFIX));
        prefixJsonElement.addProperty("color", "yellow");
        prefixJsonElement.addProperty("bold", false);
        return prefixJsonElement;
    }
}
