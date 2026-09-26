package io.github.theword.queqiao.core.runtime;

import io.github.theword.queqiao.core.config.exception.ConfigValidationException;
import io.github.theword.queqiao.core.config.io.ConfigStore;
import io.github.theword.queqiao.core.handle.HandleApiService;
import io.github.theword.queqiao.core.handle.HandleCommandReturnMessageService;
import io.github.theword.queqiao.core.protocol.handler.status.ServerStatusCollector;
import io.github.theword.queqiao.core.response.PrivateMessageResponse;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runtime 启动失败回滚测试
 *
 * <p>验证 {@link QueQiaoRuntime#start()} 的失败语义：
 * <ol>
 *     <li>已经启动的资源会被 {@code shutdown()} 清理，不残留线程与半启动对象；</li>
 *     <li>抛出的必须是<b>原始异常</b>，不被包装；</li>
 *     <li>启动失败的 Runtime 不会被平台发布（按 §19 的 create → start → publish 顺序）；</li>
 *     <li>失败之后 Runtime 仍可安全 {@code shutdown()}。</li>
 * </ol>
 *
 * <p><b>失败注入方式</b>：当前启动序列中唯一会同步抛异常的阶段是配置加载
 * （配置非法时抛出 {@link ConfigValidationException}）。为了让"清理已启动资源"这一分支
 * 真正被覆盖，本用例先用一份合法配置成功启动一次，使 Runtime 持有真实的采集线程池与
 * WebSocket 调度器，再用非法配置触发第二次启动失败，从而断言这些资源确实被回滚清理。
 *
 * <p><b>断言对象是实例自身状态</b>：本用例直接检查该 Runtime 的采集器所持有的
 * executor 是否被关闭，而不是按线程名统计 JVM 内线程数——
 * 后者会被其它用例正在退出的线程污染，导致断言不稳定。
 */
@Isolated
class QueQiaoRuntimeStartupRollbackTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(QueQiaoRuntimeStartupRollbackTest.class);

    private static final HandleApiService NOOP_API_SERVICE = new HandleApiService() {
        @Override
        public void handleBroadcastMessage(JsonElement jsonData) {
        }

        @Override
        public void handleSendTitleMessage(JsonElement titlePayload, JsonElement subTitlePayload, int fadeIn, int stay, int fadeOut) {
        }

        @Override
        public void handleSendActionBarMessage(JsonElement jsonData) {
        }

        @Override
        public PrivateMessageResponse handleSendPrivateMessage(String nickname, UUID uuid, JsonElement jsonData) {
            return null;
        }
    };

    private static final HandleCommandReturnMessageService NOOP_RETURN_MESSAGE_SERVICE =
            new HandleCommandReturnMessageService() {
                @Override
                public void handleCommandReturnMessage(Object commandReturner, String message) {
                }

                @Override
                public boolean hasPermission(Object commandReturner, String permissionNode) {
                    return true;
                }
            };

    private QueQiaoRuntime runtime;

    @AfterEach
    void tearDown() {
        QueQiaoRuntime current = runtime;
        runtime = null;
        if (current != null) {
            current.shutdown();
        }
    }

    @Test
    @DisplayName("启动失败会清理已启动资源，并抛出原始异常")
    void startFailureRollsBackAlreadyStartedResources() throws Exception {
        try (ConfigFixture fixture = new ConfigFixture()) {
            QueQiaoRuntime created = newRuntime();
            runtime = created;

            // 1. 先成功启动一次，使 Runtime 持有真实资源
            fixture.writeDisabledConfig();
            created.start();
            assertNotNull(created.getWebsocketManager(), "首次启动后应存在 WebsocketManager");
            ScheduledThreadPoolExecutor statusExecutor =
                    readRefreshExecutor(created.getServerStatusCollector());
            assertNotNull(statusExecutor, "首次启动后采集调度器应已创建");
            assertFalse(statusExecutor.isShutdown(), "首次启动后采集调度器应可用");

            // 2. 让下一次启动在配置加载阶段失败
            fixture.writeInvalidConfig();
            ConfigValidationException failure =
                    assertThrows(ConfigValidationException.class, created::start);

            // 3. 已启动资源必须被回滚清理
            assertNull(created.getWebsocketManager(), "失败回滚应关闭并清空 WebsocketManager");
            assertTrue(statusExecutor.isShutdown(), "失败回滚应关闭采集调度器，否则线程泄漏");
            assertNull(
                    readRefreshExecutor(created.getServerStatusCollector()),
                    "失败回滚应清空采集调度器引用");

            // 4. 抛出的是原始异常本身（保留类型与字段路径），不是被包装后的异常
            assertNotNull(failure.getFieldPath(), "应保留原始异常的信息，实际=" + failure.getMessage());

            // 5. 失败之后仍可安全 shutdown
            created.shutdown();
        }
    }

    @Test
    @DisplayName("按 create → start → publish 顺序，启动失败的 Runtime 不会被发布")
    void failedRuntimeIsNotPublished() throws Exception {
        try (ConfigFixture fixture = new ConfigFixture()) {
            fixture.writeInvalidConfig();

            QueQiaoRuntime published = null;
            QueQiaoRuntime candidate = newRuntime();
            runtime = candidate;
            try {
                candidate.start();
                // 只有 start() 成功才会执行到这里
                published = candidate;
            } catch (ConfigValidationException expected) {
                // 平台侧不发布失败的 Runtime
                LOGGER.info("启动按预期失败，Runtime 未被发布：{}", expected.getMessage());
            }

            assertNull(published, "启动失败的 Runtime 不应被平台发布");
            assertNull(candidate.getWebsocketManager(), "未发布的 Runtime 不应持有 WebsocketManager");
        }
    }

    private static QueQiaoRuntime newRuntime() {
        return QueQiaoRuntime.create(false, "1.20.1", "test", NOOP_API_SERVICE, NOOP_RETURN_MESSAGE_SERVICE);
    }

    /**
     * 通过反射读取采集器的私有调度器，避免为测试污染生产 API
     */
    private static ScheduledThreadPoolExecutor readRefreshExecutor(ServerStatusCollector collector) throws Exception {
        Field field = ServerStatusCollector.class.getDeclaredField("refreshExecutor");
        field.setAccessible(true);
        return (ScheduledThreadPoolExecutor) field.get(collector);
    }

    /**
     * 配置文件夹具：可写入"全部禁用"的合法配置或非法配置，结束时还原原文件
     */
    private static final class ConfigFixture implements AutoCloseable {

        private static final Path CONFIG_PATH = ConfigStore.resolveConfigPath(false);
        private static final Path BACKUP_PATH = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".bak");

        private static final String DISABLED_CONFIG = String.join("\n",
                "enable: true",
                "debug: false",
                "server_name: \"TestServer\"",
                "access_token: \"\"",
                "message_prefix: \"[鹊桥]\"",
                "enable_translation: false",
                "websocket_server:",
                "  enable: false",
                "  host: \"127.0.0.1\"",
                "  port: 8080",
                "websocket_client:",
                "  enable: false",
                "  reconnect_interval: 1",
                "  reconnect_max_times: 1",
                "  url_list: []",
                "rcon:",
                "  enable: false",
                "  port: 25575",
                "  password: \"\"",
                "subscribe_event:",
                "  player_chat: false",
                "  player_death: false",
                "  player_join: false",
                "  player_quit: false",
                "  player_command: false",
                "  player_advancement: false",
                "ignored_commands: []",
                "");

        /**
         * 根节点不是 Mapping，会被判定为 INVALID（而非 EMPTY），从而让配置加载抛异常
         */
        private static final String INVALID_CONFIG = "this-is-not-a-mapping";

        private final byte[] previousContent;

        private ConfigFixture() throws IOException {
            this.previousContent = Files.exists(CONFIG_PATH) ? Files.readAllBytes(CONFIG_PATH) : null;
            Files.createDirectories(CONFIG_PATH.getParent());
        }

        private void writeDisabledConfig() throws IOException {
            write(DISABLED_CONFIG);
        }

        private void writeInvalidConfig() throws IOException {
            write(INVALID_CONFIG);
        }

        private void write(String content) throws IOException {
            Files.write(CONFIG_PATH, content.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("已写入测试配置：{}", CONFIG_PATH.toAbsolutePath());
        }

        @Override
        public void close() throws IOException {
            if (previousContent != null) {
                Files.write(CONFIG_PATH, previousContent);
                return;
            }
            Files.deleteIfExists(CONFIG_PATH);
            Files.deleteIfExists(BACKUP_PATH);
        }
    }
}
