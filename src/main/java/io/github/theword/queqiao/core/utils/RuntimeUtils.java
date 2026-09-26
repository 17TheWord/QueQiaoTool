package io.github.theword.queqiao.core.utils;

import io.github.theword.queqiao.core.config.Config;
import io.github.theword.queqiao.core.config.ConfigKeys;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Runtime 作用域辅助工具
 *
 * <p>保存"属于当前 QueQiao Runtime、但无法自然归属于某一个具体服务"的零碎辅助能力。
 *
 * <p><b>依赖方向</b>：{@code QueQiaoRuntime → RuntimeUtils → Config / Logger}。
 * 本类<b>不得</b>持有 {@link io.github.theword.queqiao.core.runtime.QueQiaoRuntime}，
 * 否则会形成 Runtime 与 RuntimeUtils 之间的循环依赖。
 *
 * <p><b>不缓存配置</b>：所有判定都在调用时读取当前 {@link Config}。
 * 由于 {@code Config.load()} 是原地更新同一个实例，配置 reload 后本类自动生效，
 * 不需要额外的 {@code reload()} / {@code updateIgnoredCommands()} 之类的同步入口，
 * 也不会产生一份过期的本地配置副本。
 *
 * <p><b>logger 是构造期快照</b>：{@code logger} 在构造时被捕获，之后不再变化。
 * 因此 {@code QueQiaoRuntime.setLogger(...)} 之后，本类仍会用<b>旧</b>的 logger 输出 debug 日志。
 * 这是与配置行为不同的一点（配置是原地更新，故 reload 后自动生效）；
 * 由于 {@code setLogger} 仅为兼容保留、生产代码没有调用方，该差异当前无实际影响。
 *
 * @since 0.7.0
 */
public final class RuntimeUtils {

    private final Config config;
    private final Logger logger;

    /**
     * 构造 Runtime 作用域工具
     *
     * @param config 配置运行时状态，不得为 null
     * @param logger 日志实现，不得为 null
     */
    public RuntimeUtils(Config config, Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * 判断是否为需要忽略的命令
     *
     * <p><b>返回值语义保持不变</b>（勿改为 boolean）：
     * 该方法是面向平台适配器的公开能力，调用方依赖"空串表示忽略"这一约定。
     *
     * @param command 命令
     * @return 如果是需要忽略的命令，返回空字符串；否则返回归一化后的命令
     * @since 0.4.2
     */
    public String isIgnoredCommand(String command) {
        if (command == null) return "";

        command = command.trim();
        if (command.isEmpty()) return "";

        if (command.startsWith("/")) command = command.substring(1);

        String commandHeader = command.split(" ", 2)[0].toLowerCase();
        if (ConfigKeys.effectiveIgnoredCommands(config).contains(commandHeader)) return "";
        return command;
    }

    /**
     * DEBUG 模式 用于输出更多内容
     *
     * @param message 消息
     */
    public void debugLog(String message) {
        if (!isDebugEnabled()) {
            return;
        }
        logger.info("[DEBUG] " + message);
    }

    /**
     * DEBUG 模式 用于输出更多内容
     *
     * @param format 格式
     * @param args   参数
     */
    public void debugLog(String format, Object... args) {
        if (!isDebugEnabled()) {
            return;
        }
        logger.info("[DEBUG] " + format, args);
    }

    /**
     * 判断是否可以输出调试日志
     *
     * <p>对外暴露是为了让调用方在构造昂贵的日志参数（如脱敏后的请求体）之前先判断，
     * 避免"日志关闭却仍付出构造代价"。
     *
     * @return true 表示开启了 debug
     */
    public boolean isDebugEnabled() {
        return Boolean.TRUE.equals(config.get(ConfigKeys.DEBUG));
    }
}
