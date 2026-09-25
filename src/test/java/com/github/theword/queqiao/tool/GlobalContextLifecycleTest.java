package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.ConfigKeys;
import com.github.theword.queqiao.tool.event.PlayerChatEvent;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.handle.HandleCommandReturnMessageService;
import com.github.theword.queqiao.tool.response.PrivateMessageResponse;
import com.github.theword.queqiao.tool.runtime.QueQiaoRuntime;
import com.github.theword.queqiao.tool.utils.WebsocketManager;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局上下文生命周期测试（G2 / G3）
 *
 * <p>覆盖两项 P0 缺陷的回归：
 * <ul>
 *     <li><b>G3</b>：运行时空对象此前是"全字段为 null"的失效对象，
 *         导致未初始化时 {@code shutdown()} / {@code sendEvent()} 直接 NPE。</li>
 *     <li><b>G2</b>：{@code init()} 此前没有幂等保护，重复调用会覆盖 runtime，
 *         旧实例持有的 WebSocket 连接、共享重连调度器、Rcon 连接与线程永久泄漏。</li>
 * </ul>
 *
 * <p><b>关于 G1</b>（{@code runtime} 的跨线程可见性）：它是 JMM 层面的属性，
 * 无法用单测稳定复现——无论是否加 {@code volatile}，普通测试通常都会通过。
 * 因此 G1 由代码审查与 {@code volatile} / {@code final} 的显式声明保证，不在本类覆盖。
 *
 * <p><b>测试隔离说明</b>：{@code init()} 会读取工作目录下的
 * {@code plugins/queqiao/config.yml}，本类通过写入"全部禁用"的配置夹具
 * （并在结束时还原原文件）来避免绑定端口或产生网络副作用。
 * 这是已知的测试隔离欠债，后续批次应改为可注入配置目录。
 */
class GlobalContextLifecycleTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalContextLifecycleTest.class);

    /**
     * 平台侧 API 实现，测试中不需要真实行为
     */
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

    /**
     * 平台侧命令返回消息实现，测试中不需要真实行为
     */
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

    @AfterEach
    void tearDown() {
        // 保证不把已初始化的运行时留给后续测试类
        GlobalContext.shutdown();
    }

    // ------------------------------------------------------------------
    // G3：空对象必须真正可用
    // ------------------------------------------------------------------

    @Test
    @DisplayName("未初始化时 getLogger 与 getConfig 不返回 null（G3 回归）")
    void loggerAndConfigAreNeverNullBeforeInit() {
        assertNotNull(GlobalContext.getLogger(), "getLogger() 不应返回 null");
        assertNotNull(GlobalContext.getConfig(), "getConfig() 不应返回 null");
        assertNotNull(GlobalContext.getGson(), "getGson() 不应返回 null");
    }

    @Test
    @DisplayName("未初始化时 shutdown 不抛异常（G3 回归）")
    void shutdownBeforeInitIsSafe() {
        GlobalContext.shutdown();
        GlobalContext.shutdown();
    }

    @Test
    @DisplayName("未初始化时 sendEvent 不抛异常（G3 回归）")
    void sendEventBeforeInitIsSafe() {
        GlobalContext.sendEvent(new PlayerChatEvent(null, "", "", "hello"));
    }

    @Test
    @DisplayName("未初始化时 translate 返回原 key，不抛异常（G3 回归）")
    void translateBeforeInitReturnsKey() {
        assertEquals("death.attack.player", GlobalContext.translate("death.attack.player", new String[] {"Steve"}));
        assertFalse(GlobalContext.isTranslationEnabled(), "未初始化时翻译应为关闭状态");
    }

    @Test
    @DisplayName("默认配置不触碰文件系统，且忽略命令已预置（G3 配套）")
    void emptyRuntimeConfigHasDefaultIgnoredCommands() {
        Config config = QueQiaoRuntime.empty().getConfig();

        assertNotNull(config, "空对象的 config 不应为 null");
        Set<String> ignored = ConfigKeys.effectiveIgnoredCommands(config);
        assertNotNull(ignored, "忽略命令集合不应为 null");
        assertTrue(ignored.contains("login"), "默认配置应预置登录命令为忽略项，实际=" + ignored);
        assertTrue(ignored.contains("register"), "默认配置应预置注册命令为忽略项");
    }

    @Test
    @DisplayName("空对象的 getConfig 可被安全读取常用字段")
    void emptyRuntimeConfigFieldsAreUsable() {
        Config config = QueQiaoRuntime.empty().getConfig();

        assertTrue(config.get(ConfigKeys.ENABLE));
        assertNotNull(config.get(ConfigKeys.WebSocketClient.URL_LIST));
        assertEquals("127.0.0.1", config.get(ConfigKeys.WebSocket.HOST));
        assertEquals("", config.get(ConfigKeys.ACCESS_TOKEN));
    }

    // ------------------------------------------------------------------
    // E1：API 边界的必填依赖校验
    // ------------------------------------------------------------------

    /**
     * E1 回归：平台实现为 null 时必须在 {@code init()} 入口立即失败并指明参数名。
     *
     * <p>若不在此处拦截，会拖到"第一条协议请求"或"第一条命令"执行时才抛 NPE，定位成本很高。
     */
    @Test
    @DisplayName("init 时平台实现为 null 立即失败并指明参数名（E1）")
    void initRejectsNullPlatformImplementations() {
        NullPointerException apiException = assertThrows(
                NullPointerException.class,
                () -> GlobalContext.init(false, "1.20.1", "test", null, NOOP_RETURN_MESSAGE_SERVICE));
        assertTrue(
                apiException.getMessage() != null && apiException.getMessage().contains("handleApiImpl"),
                "错误信息应指明参数名，实际=" + apiException.getMessage());

        NullPointerException serviceException = assertThrows(
                NullPointerException.class,
                () -> GlobalContext.init(false, "1.20.1", "test", NOOP_API_SERVICE, null));
        assertTrue(
                serviceException.getMessage() != null
                        && serviceException.getMessage().contains("handleCommandReturnMessageImpl"),
                "错误信息应指明参数名，实际=" + serviceException.getMessage());
    }

    // ------------------------------------------------------------------
    // G2：重复 init 必须关闭旧实例
    // ------------------------------------------------------------------

    @Test
    @DisplayName("重复 init 会关闭旧实例的共享调度器，不泄漏资源（G2 回归）")
    void repeatedInitShutsDownPreviousRuntime() throws Exception {
        try (DisabledConfigFixture ignored = new DisabledConfigFixture()) {
            GlobalContext.init(false, "1.20.1", "test", NOOP_API_SERVICE, NOOP_RETURN_MESSAGE_SERVICE);
            WebsocketManager firstManager = GlobalContext.getWebsocketManager();
            assertNotNull(firstManager, "首次 init 后应存在 WebsocketManager");
            ScheduledThreadPoolExecutor firstScheduler = readReconnectScheduler(firstManager);
            assertFalse(firstScheduler.isShutdown(), "首次 init 的共享调度器应可用");

            GlobalContext.init(false, "1.20.1", "test", NOOP_API_SERVICE, NOOP_RETURN_MESSAGE_SERVICE);

            assertTrue(
                    firstScheduler.isShutdown(),
                    "重复 init 必须关闭旧实例的共享调度器，否则其线程与连接会永久泄漏");
            assertNotSame(firstManager, GlobalContext.getWebsocketManager(), "应已替换为新的 WebsocketManager");
        }
    }

    @Test
    @DisplayName("shutdown 可重复调用，且关闭后仍可安全读取上下文（G2/G3 组合）")
    void shutdownIsIdempotentAndLeavesContextUsable() throws Exception {
        try (DisabledConfigFixture ignored = new DisabledConfigFixture()) {
            GlobalContext.init(false, "1.20.1", "test", NOOP_API_SERVICE, NOOP_RETURN_MESSAGE_SERVICE);
            WebsocketManager manager = GlobalContext.getWebsocketManager();
            ScheduledThreadPoolExecutor scheduler = readReconnectScheduler(manager);

            GlobalContext.shutdown();
            assertTrue(scheduler.isShutdown(), "shutdown 应释放共享调度器");

            // 再次 shutdown 不应抛异常
            GlobalContext.shutdown();

            // 关闭后回到最小可用运行时，读取与事件分发仍然安全
            assertNotNull(GlobalContext.getLogger());
            assertNotNull(GlobalContext.getConfig());
            GlobalContext.sendEvent(new PlayerChatEvent(null, "", "", "after shutdown"));
        }
    }

    /**
     * 通过反射读取 Manager 的私有调度器，避免为测试污染生产 API
     */
    private static ScheduledThreadPoolExecutor readReconnectScheduler(WebsocketManager manager) throws Exception {
        Field field = WebsocketManager.class.getDeclaredField("reconnectScheduler");
        field.setAccessible(true);
        return (ScheduledThreadPoolExecutor) field.get(manager);
    }

    /**
     * "全部禁用"配置夹具
     *
     * <p>把 WebSocket Server / Client / Rcon 全部关闭，使 {@code init()} 不绑定端口、
     * 不建立任何连接。构造时备份原配置文件，{@link #close()} 时还原。
     */
    private static final class DisabledConfigFixture implements AutoCloseable {

        private static final Path CONFIG_PATH = Paths.get("plugins", "queqiao", "config.yml");
        private static final Path BACKUP_PATH = Paths.get("plugins", "queqiao", "config.yml.bak");

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

        private final byte[] previousContent;

        private DisabledConfigFixture() throws IOException {
            this.previousContent = Files.exists(CONFIG_PATH) ? Files.readAllBytes(CONFIG_PATH) : null;
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, DISABLED_CONFIG.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("已写入禁用全部传输层的测试配置：{}", CONFIG_PATH.toAbsolutePath());
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