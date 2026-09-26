package io.github.theword.queqiao.core.protocol.handler.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ServerStatusCollector} 快照缓存测试（WS-B6）
 *
 * <p>背景：{@code get_status} 会在连接读线程上同步执行 Minecraft Server List Ping
 * （socket 超时 3 秒），属潜在阻塞调用。加入短 TTL 缓存后，突发请求被收敛为一次采集。
 *
 * <p>采集器已改为<b>实例级</b>：本用例为每个测试方法构造一个独立实例，
 * 缓存不再跨用例共享（JUnit 默认每个测试方法新建一次测试类实例）。
 * 用例不调用 {@code startRefreshScheduler}，因此走同步采集回退分支；
 * 默认探测目标为"不可用"，不会发起真实 Ping。
 */
class ServerStatusCollectorCacheTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStatusCollectorCacheTest.class);

    /**
     * 与实现中的 SNAPSHOT_CACHE_TTL_MILLIS 保持一致，留出余量
     */
    private static final long TTL_MILLIS = 2000L;

    private static final String FIELD_TIMESTAMP = "timestamp";

    /**
     * 未启动的采集器：走同步采集回退，且不绑定任何后台线程
     */
    private final ServerStatusCollector collector = new ServerStatusCollector(null, null, LOGGER);

    @Test
    @DisplayName("TTL 内重复调用命中缓存，返回同一快照")
    void repeatedCallsWithinTtlReturnSameSnapshot() {
        Map<String, Object> first = collector.collectStatusSnapshot();
        Map<String, Object> second = collector.collectStatusSnapshot();

        assertNotNull(first);
        assertNotNull(second);
        assertNotNull(first.get(FIELD_TIMESTAMP), "快照应包含 timestamp");
        assertEquals(
                first.get(FIELD_TIMESTAMP), second.get(FIELD_TIMESTAMP),
                "TTL 内应命中缓存，timestamp 应相同");
    }

    @Test
    @DisplayName("快照包含全部预期分区")
    void snapshotContainsExpectedSections() {
        Map<String, Object> snapshot = collector.collectStatusSnapshot();

        assertTrue(snapshot.containsKey(FIELD_TIMESTAMP), "缺少 timestamp");
        assertTrue(snapshot.containsKey("server_type"), "缺少 server_type");
        assertTrue(snapshot.containsKey("server_version"), "缺少 server_version");
        assertTrue(snapshot.containsKey("server_list_ping"), "缺少 server_list_ping");
        assertTrue(snapshot.containsKey("cpu_information"), "缺少 cpu_information");
        assertTrue(snapshot.containsKey("memory_information"), "缺少 memory_information");
    }

    @Test
    @DisplayName("超过 TTL 后重新采集，timestamp 变化")
    void expiredCacheTriggersRefresh() throws InterruptedException {
        Map<String, Object> first = collector.collectStatusSnapshot();

        Thread.sleep(TTL_MILLIS + 300L);

        Map<String, Object> second = collector.collectStatusSnapshot();

        assertNotEquals(
                first.get(FIELD_TIMESTAMP), second.get(FIELD_TIMESTAMP),
                "TTL 过期后应重新采集，timestamp 应变化");
    }

    @Test
    @DisplayName("刷新探测目标后缓存失效")
    void changingPingTargetInvalidatesCache() {
        Map<String, Object> before = collector.collectStatusSnapshot();
        assertNotNull(before.get(FIELD_TIMESTAMP));

        // initPingTarget 会重新解析 server.properties 并清空缓存
        collector.initPingTarget();

        Map<String, Object> after = collector.collectStatusSnapshot();
        assertNotNull(after.get(FIELD_TIMESTAMP), "刷新目标后应能重新采集");
    }
}
