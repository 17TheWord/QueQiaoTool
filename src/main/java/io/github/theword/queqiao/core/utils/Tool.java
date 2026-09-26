package io.github.theword.queqiao.core.utils;

import io.github.theword.queqiao.core.constant.BaseConstant;

/**
 * 纯工具类
 *
 * <p><b>无状态</b>：本类只提供不依赖任何 Runtime 上下文的纯函数，
 * 不得访问 Config / Logger / Runtime，也不得持有 Runtime 引用
 * （否则会重新变成隐式的全局上下文）。
 *
 * <p>需要 Runtime 上下文的辅助能力请使用
 * {@link io.github.theword.queqiao.core.utils.RuntimeUtils}。
 */
public class Tool {

    /**
     * 判断是否为注册或登录命令
     *
     * @param command 命令
     * @return 是否为注册或登录命令，如果是，返回空字符串
     * @deprecated 0.4.2，请使用 {@link RuntimeUtils#isIgnoredCommand(String)} 代替
     */
    @Deprecated
    public static String isRegisterOrLoginCommand(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        if (command.startsWith("l ") || command.startsWith("login ") || command.startsWith("register ") || command.startsWith("reg ") || command.startsWith(BaseConstant.COMMAND_HEADER + " "))
            return "";
        return command;
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
        if (args == null || args.length == 0 || !template.contains("{}")) {
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
}
