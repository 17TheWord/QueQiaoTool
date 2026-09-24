package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.handle.HandleProtocolMessage;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebsocketManager 调度器所有权与生命周期测试
 *
 * <p>验证 WS-A4 的所有权约束：
 * <ul>
 *     <li>调度器在构造阶段创建（{@link ScheduledThreadPoolExecutor} 的 core 线程在首次提交任务时才启动，
 *         因此构造后无实际线程开销）</li>
 *     <li>{@code stop()} 是唯一释放调度器的路径</li>
 *     <li>{@code stop()} 幂等</li>
 * </ul>
 *
 * <p>说明：{@code start()} 的幂等性与 endpoint 隔离未在此处覆盖——
 * 它们依赖 {@code GlobalContext} 配置，而现有配置加载会读写工作目录下的
 * {@code plugins/queqiao/config.yml} 并可能绑定真实端口，属于已知的测试隔离问题，
 * 待测试基础设施批次处理。
 */
class WebsocketManagerLifecycleTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketManagerLifecycleTest.class);
    private static final Gson GSON = new Gson();

    /**
     * 与生产一致：协议分发入口由上层创建后注入 Manager
     */
    private static final HandleProtocolMessage HANDLE_PROTOCOL_MESSAGE = new HandleProtocolMessage(LOGGER, GSON);

    /**
     * 通过反射读取私有字段，避免为测试污染生产 API
     */
    private static ScheduledThreadPoolExecutor readReconnectScheduler(WebsocketManager manager) throws Exception {
        Field field = WebsocketManager.class.getDeclaredField("reconnectScheduler");
        field.setAccessible(true);
        return (ScheduledThreadPoolExecutor) field.get(manager);
    }

    @Test
    @DisplayName("构造阶段即创建共享调度器，且未提交任务")
    void schedulerIsCreatedInConstructor() throws Exception {
        WebsocketManager manager = new WebsocketManager(LOGGER, GSON, new NoopReturnMessageService(), HANDLE_PROTOCOL_MESSAGE);
        try {
            ScheduledThreadPoolExecutor scheduler = readReconnectScheduler(manager);
            assertNotNull(scheduler, "构造后应已持有共享调度器");
            assertFalse(scheduler.isShutdown(), "构造后调度器应可用");
            assertEquals(2, scheduler.getCorePoolSize(), "corePoolSize 应为 2");
            assertTrue(scheduler.getRemoveOnCancelPolicy(), "应启用 removeOnCancelPolicy");
            assertFalse(
                    scheduler.getExecuteExistingDelayedTasksAfterShutdownPolicy(),
                    "关闭后不应继续执行已排队的延迟任务");
            assertTrue(scheduler.getQueue().isEmpty(), "构造后不应有排队任务");
        } finally {
            manager.stop(1000, "test cleanup", null);
        }
    }

    @Test
    @DisplayName("stop() 释放共享调度器，且重复调用幂等")
    void stopShutsDownSchedulerAndIsIdempotent() throws Exception {
        WebsocketManager manager = new WebsocketManager(LOGGER, GSON, new NoopReturnMessageService(), HANDLE_PROTOCOL_MESSAGE);
        ScheduledThreadPoolExecutor scheduler = readReconnectScheduler(manager);
        assertFalse(scheduler.isShutdown(), "初始状态不应为已关闭");

        manager.stop(1000, "test", null);
        assertTrue(scheduler.isShutdown(), "stop() 应释放共享调度器");

        // 幂等：重复调用不得抛异常，状态保持已关闭
        manager.stop(1000, "test", null);
        assertTrue(scheduler.isShutdown(), "重复 stop() 后调度器仍应处于已关闭状态");
    }

    @Test
    @DisplayName("stop() 之后 restart() 不抛异常（已销毁保护）")
    void restartAfterStopIsSafe() throws Exception {
        WebsocketManager manager = new WebsocketManager(LOGGER, GSON, new NoopReturnMessageService(), HANDLE_PROTOCOL_MESSAGE);
        manager.stop(1000, "test", null);

        // 已销毁后重载应被安全忽略，而不是复用已关闭的 scheduler
        manager.restart(null);
        assertTrue(readReconnectScheduler(manager).isShutdown(), "已销毁后不应复活调度器");
    }

    /**
     * 空实现，避免测试依赖平台实现
     */
    private static final class NoopReturnMessageService extends HandleCommandReturnMessageService {

        @Override
        public void handleCommandReturnMessage(Object commandReturner, String message) {
        }

        @Override
        public boolean hasPermission(Object commandReturner, String permissionNode) {
            return true;
        }
    }
}
