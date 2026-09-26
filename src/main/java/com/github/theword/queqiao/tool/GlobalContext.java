package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.schema.ConfigRegistry;
import com.github.theword.queqiao.tool.event.base.BaseEvent;
import com.github.theword.queqiao.tool.exception.rcon.RconException;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.runtime.QueQiaoRuntime;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 全局上下文门面
 *
 * <p>对外提供静态访问入口，内部持有一个 {@link QueQiaoRuntime} 实例。
 *
 * <p><b>线程安全</b>：
 * <ul>
 *     <li>{@code runtime} 为 {@code volatile}，保证初始化后的引用对所有线程可见（安全发布）</li>
 *     <li>{@link #init} / {@link #shutdown} 由 {@code INIT_LOCK} 串行化，二者均幂等</li>
 * </ul>
 *
 * <p><b>未初始化状态</b>：{@code runtime} 初始为 {@link QueQiaoRuntime#empty()}——
 * 一个"最小可用运行时"（NOP 日志 + 默认配置），
 * 因此 {@code getLogger()} / {@code getConfig()} 不会返回 null，
 * {@code shutdown()} / {@code sendEvent()} 也不会抛异常。
 *
 * @since 0.6.11
 */
public final class GlobalContext {

    /**
     * 串行化 init / shutdown 的锁
     */
    private static final Object INIT_LOCK = new Object();

    /**
     * 当前运行时
     *
     * <p>volatile 是必须的：该引用由调用 {@code init()} 的线程写入
     * （通常是服务端启动线程），而由游戏主线程与 WebSocket 读写线程读取。
     * 非 volatile 时其它线程可能读到旧引用或半初始化对象，且难以复现。
     */
    private static volatile QueQiaoRuntime runtime = QueQiaoRuntime.empty();

    /**
     * 是否已初始化
     */
    private static boolean initialized = false;

    private GlobalContext() {
    }

    /**
     * 初始化鹊桥
     *
     * <p><b>幂等</b>：若已初始化，会先关闭旧实例再创建新实例。
     * 这样即使平台在未调用 {@code shutdown()} 的情况下重复初始化（如插件热重载），
     * 旧实例持有的 WebSocket 连接、共享重连调度器、Rcon 连接与线程也能被正确回收，
     * 而不是变成无法再关闭的孤儿对象。
     *
     * @param isModServer                       是否为模组服务端
     * @param serverVersion                     服务端版本
     * @param serverType                        服务端类型
     * @param handleApiImpl                     平台 API 实现
     * @param handleCommandReturnMessageImpl    平台命令返回消息实现
     */
    public static void init(boolean isModServer, String serverVersion, String serverType, HandleApiService handleApiImpl, HandleCommandReturnMessageService handleCommandReturnMessageImpl) {
        init(isModServer, serverVersion, serverType, handleApiImpl, handleCommandReturnMessageImpl, null);
    }

    /**
     * 初始化并在配置加载前注册扩展配置。
     */
    public static void init(
            boolean isModServer,
            String serverVersion,
            String serverType,
            HandleApiService handleApiImpl,
            HandleCommandReturnMessageService handleCommandReturnMessageImpl,
            Consumer<ConfigRegistry> configurer) {
        // 平台实现是 API 边界的必填依赖。为 null 时若不在此处拦截，
        // 会拖到"第一条协议请求"或"第一条命令"执行时才抛 NPE，定位成本很高；
        // 在入口快速失败并给出明确参数名，可让接线错误在启动阶段立刻暴露。
        Objects.requireNonNull(handleApiImpl, "handleApiImpl 不能为 null：平台必须提供 HandleApiService 实现");
        Objects.requireNonNull(handleCommandReturnMessageImpl, "handleCommandReturnMessageImpl 不能为 null：平台必须提供 HandleCommandReturnMessageService 实现");

        synchronized (INIT_LOCK) {
            if (initialized) {
                getLogger().warn("鹊桥已被初始化过，将先关闭旧实例再重新初始化，以避免 WebSocket / Rcon / 线程泄漏");
                shutdownInternal();
            }
            QueQiaoRuntime newRuntime = QueQiaoRuntime.create(
                    isModServer, serverVersion, serverType, handleApiImpl, handleCommandReturnMessageImpl, configurer);
            runtime = newRuntime;
            initialized = true;
            newRuntime.start();
        }
    }

    public static void executeReloadCommand(Object commandReturner) {
        runtime.reload(commandReturner);
    }

    /**
     * 关闭鹊桥
     *
     * <p><b>幂等</b>：未初始化时调用、或重复调用，都不会抛异常。
     */
    public static void shutdown() {
        synchronized (INIT_LOCK) {
            shutdownInternal();
        }
    }

    /**
     * 关闭当前运行时的内部实现
     *
     * <p>调用方必须持有 {@link #INIT_LOCK}。
     * 关闭后把 {@code runtime} 复位为最小可用运行时，使后续调用仍然安全。
     */
    private static void shutdownInternal() {
        if (!initialized) {
            return;
        }
        runtime.shutdown();
        runtime = QueQiaoRuntime.empty();
        initialized = false;
    }

    public static void sendEvent(BaseEvent baseEvent) {
        runtime.sendEvent(baseEvent);
    }

    public static String sendRconCommand(String command) throws RconException {
        return runtime.sendRconCommand(command);
    }

    public static JsonElement initMessagePrefixJsonObject(String messagePrefixText) {
        return runtime.initMessagePrefixJsonObject(messagePrefixText);
    }

    public static boolean isTranslationEnabled() {
        return runtime.isTranslationEnabled();
    }

    public static String translate(String key, String[] args) {
        return runtime.translate(key, args);
    }

    /**
     * @return 配置运行时状态（<b>唯一</b>配置状态来源）
     */
    public static Config getConfig() {
        return runtime.getConfig();
    }

    /**
     * 替换运行时配置状态（供测试与工具注入）
     *
     * @param config 新的配置运行时状态
     */
    public static void setConfig(Config config) {
        runtime.setConfig(config);
    }

    public static Logger getLogger() {
        return runtime.getLogger();
    }

    public static void setLogger(Logger logger) {
        runtime.setLogger(logger);
    }

    public static WebsocketManager getWebsocketManager() {
        return runtime.getWebsocketManager();
    }

    public static HandleApiService getHandleApiService() {
        return runtime.getHandleApiService();
    }

    public static HandleCommandReturnMessageService getHandleCommandReturnMessageService() {
        return runtime.getHandleCommandReturnMessageService();
    }

    public static String getServerVersion() {
        return runtime.getServerVersion();
    }

    public static String getServerType() {
        return runtime.getServerType();
    }

    public static Gson getGson() {
        return runtime.getGson();
    }

    public static JsonElement getMessagePrefixJsonObject() {
        return runtime.getMessagePrefixJsonElement();
    }
}
