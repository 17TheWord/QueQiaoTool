package io.github.theword.queqiao.core.support;

import io.github.theword.queqiao.core.config.Config;
import io.github.theword.queqiao.core.config.ConfigKeys;
import io.github.theword.queqiao.core.config.schema.ConfigRegistry;
import io.github.theword.queqiao.core.exception.rcon.RconException;
import io.github.theword.queqiao.core.handle.HandleApiService;
import io.github.theword.queqiao.core.handle.HandleProtocolMessage;
import io.github.theword.queqiao.core.protocol.RconCommandExecutor;
import io.github.theword.queqiao.core.protocol.handler.status.ServerStatusCollector;
import io.github.theword.queqiao.core.response.PrivateMessageResponse;
import io.github.theword.queqiao.core.utils.RuntimeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 协议层测试替身
 *
 * <p>协议层通过构造器接收"平台 API 实现"与"RCON 执行器"，
 * 因此测试可以注入替身来验证<b>成功路径</b>——
 * 这在依赖静态全局状态的时期是做不到的（平台实现为 null，只能落到 500）。
 */
public final class PlatformStubs {

    private PlatformStubs() {
    }

    /**
     * 不做任何事、返回 null 的平台 API 实现
     */
    public static HandleApiService noopApiService() {
        return new RecordingApiService();
    }

    /**
     * 记录所有调用的平台 API 实现
     */
    public static RecordingApiService recordingApiService() {
        return new RecordingApiService();
    }

    /**
     * 不做任何事的 RCON 执行器，返回固定结果
     *
     * @param result 返回值
     */
    public static RconCommandExecutor rconExecutorReturning(String result) {
        return command -> result;
    }

    /**
     * 总是抛出指定类型 RconException 的执行器
     *
     * @param kind 失败类型
     */
    public static RconCommandExecutor rconExecutorFailing(RconException.Kind kind) {
        return command -> {
            switch (kind) {
                case DISABLED:
                    throw RconException.disabled();
                case DISCONNECTED:
                    throw RconException.disconnected();
                case INVALID_COMMAND:
                    throw RconException.invalidCommand();
                case COMMAND_FAILED:
                default:
                    throw RconException.commandFailed(new IllegalStateException("stub failure"));
            }
        };
    }

    /**
     * 构造一份不触碰文件系统的默认配置（默认值只来自 {@link ConfigKeys} 的 Schema）
     */
    public static Config defaultConfig() {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKeys.registerAll(registry);
        return new Config(registry);
    }

    /**
     * 构造 Runtime 作用域辅助能力（默认配置 + 给定日志实现）
     */
    public static RuntimeUtils newRuntimeUtils(Logger logger) {
        return new RuntimeUtils(defaultConfig(), logger);
    }

    /**
     * 构造未启动的状态采集器（未调用 startRefreshScheduler，走同步采集回退分支）
     */
    public static ServerStatusCollector newStatusCollector(Logger logger) {
        return new ServerStatusCollector(null, null, logger);
    }

    /**
     * 构造协议分发入口（使用空平台实现与空 RCON 执行器）
     */
    public static HandleProtocolMessage newDispatcher(Logger logger, Gson gson) {
        return new HandleProtocolMessage(
                logger, gson, noopApiService(), rconExecutorReturning(""), newRuntimeUtils(logger), newStatusCollector(logger));
    }

    /**
     * 构造协议分发入口（指定平台实现与 RCON 执行器）
     */
    public static HandleProtocolMessage newDispatcher(
            Logger logger, Gson gson, HandleApiService apiService, RconCommandExecutor rconCommandExecutor) {
        return new HandleProtocolMessage(
                logger, gson, apiService, rconCommandExecutor, newRuntimeUtils(logger), newStatusCollector(logger));
    }

    /**
     * 记录调用的平台 API 实现
     */
    public static final class RecordingApiService implements HandleApiService {

        private final List<String> broadcasts = Collections.synchronizedList(new ArrayList<>());
        private final List<String> actionBars = Collections.synchronizedList(new ArrayList<>());
        private final List<String> titleCalls = Collections.synchronizedList(new ArrayList<>());
        private final List<String> privateMessages = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void handleBroadcastMessage(JsonElement jsonData) {
            broadcasts.add(String.valueOf(jsonData));
        }

        @Override
        public void handleSendTitleMessage(JsonElement titlePayload, JsonElement subTitlePayload, int fadeIn, int stay, int fadeOut) {
            titleCalls.add("title=" + titlePayload + ", subtitle=" + subTitlePayload
                    + ", fadeIn=" + fadeIn + ", stay=" + stay + ", fadeOut=" + fadeOut);
        }

        @Override
        public void handleSendActionBarMessage(JsonElement jsonData) {
            actionBars.add(String.valueOf(jsonData));
        }

        @Override
        public PrivateMessageResponse handleSendPrivateMessage(String nickname, UUID uuid, JsonElement jsonData) {
            privateMessages.add("nickname=" + nickname + ", uuid=" + uuid + ", message=" + jsonData);
            return null;
        }

        public List<String> getBroadcasts() {
            synchronized (broadcasts) {
                return new ArrayList<>(broadcasts);
            }
        }

        public List<String> getActionBars() {
            synchronized (actionBars) {
                return new ArrayList<>(actionBars);
            }
        }

        public List<String> getTitleCalls() {
            synchronized (titleCalls) {
                return new ArrayList<>(titleCalls);
            }
        }

        public List<String> getPrivateMessages() {
            synchronized (privateMessages) {
                return new ArrayList<>(privateMessages);
            }
        }
    }
}
