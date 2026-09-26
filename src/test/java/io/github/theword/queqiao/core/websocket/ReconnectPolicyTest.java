package io.github.theword.queqiao.core.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ReconnectPolicy} 边界测试
 *
 * <p>回归目标：
 * <ul>
 *     <li>负延迟永不产生（旧实现 {@code 1L << 63} 溢出为负 → scheduler 立即执行 → 重连风暴）</li>
 *     <li>移位溢出与乘法溢出均不存在</li>
 *     <li>极端 attempt（Integer.MAX_VALUE）不会退化为长循环或负值</li>
 * </ul>
 */
class ReconnectPolicyTest {

    private static final long MIN = ReconnectPolicy.MIN_RECONNECT_DELAY_SECONDS;
    private static final long MAX = ReconnectPolicy.MAX_RECONNECT_DELAY_SECONDS;

    /**
     * 所有边界 attempt 都必须产出合法延迟
     */
    @Test
    @DisplayName("任何间隔配置下延迟恒在合法区间内")
    void delayAlwaysWithinBoundsForAnyInterval() {
        int[] intervals = {-1, 0, 1, 5, 60, 61, Integer.MAX_VALUE};
        int[] attempts = {0, 1, 2, 5, 10, 30, 62, 63, 64, 1000, Integer.MAX_VALUE};

        for (int interval : intervals) {
            ReconnectPolicy policy = new ReconnectPolicy(interval, 5);
            for (int attempt : attempts) {
                long delay = policy.calculateReconnectDelay(attempt);
                assertTrue(delay >= MIN, "attempt=" + attempt + " interval=" + interval + " 产生了低于下限的延迟: " + delay);
                assertTrue(delay <= MAX, "attempt=" + attempt + " interval=" + interval + " 产生了超过上限的延迟: " + delay);
            }
        }
    }

    /**
     * 负数间隔归一化到下限（旧实现会把负延迟直接交给 scheduler）
     */
    @Test
    @DisplayName("负数间隔归一化为最小间隔")
    void negativeIntervalIsNormalizedToMinimum() {
        ReconnectPolicy policy = new ReconnectPolicy(-1, 5);
        assertEquals(MIN, policy.getBaseIntervalSeconds());
        assertEquals(MIN, policy.calculateReconnectDelay(0));
        assertTrue(policy.calculateReconnectDelay(63) >= MIN);
    }

    /**
     * 0 间隔归一化到下限
     */
    @Test
    @DisplayName("0 间隔归一化为最小间隔")
    void zeroIntervalIsNormalizedToMinimum() {
        ReconnectPolicy policy = new ReconnectPolicy(0, 5);
        assertEquals(MIN, policy.getBaseIntervalSeconds());
        assertEquals(MIN, policy.calculateReconnectDelay(0));
    }

    /**
     * 超大间隔归一化到上限，不会因乘法溢出产生负数
     */
    @Test
    @DisplayName("超大间隔归一化为最大间隔且不溢出")
    void hugeIntervalIsNormalizedToMaximum() {
        ReconnectPolicy policy = new ReconnectPolicy(Integer.MAX_VALUE, 5);
        assertEquals(MAX, policy.getBaseIntervalSeconds());
        assertEquals(MAX, policy.calculateReconnectDelay(0));
        assertEquals(MAX, policy.calculateReconnectDelay(Integer.MAX_VALUE));
    }

    /**
     * 指数退避的具体取值：base=1 → 1,2,4,8,16,32,60,60…
     */
    @Test
    @DisplayName("指数退避取值正确并在上限处封顶")
    void exponentialBackoffValuesAreCorrect() {
        ReconnectPolicy policy = new ReconnectPolicy(1, 100);

        assertEquals(1L, policy.calculateReconnectDelay(0));
        assertEquals(2L, policy.calculateReconnectDelay(1));
        assertEquals(4L, policy.calculateReconnectDelay(2));
        assertEquals(8L, policy.calculateReconnectDelay(3));
        assertEquals(16L, policy.calculateReconnectDelay(4));
        assertEquals(32L, policy.calculateReconnectDelay(5));
        assertEquals(60L, policy.calculateReconnectDelay(6));
        assertEquals(60L, policy.calculateReconnectDelay(7));
    }

    /**
     * 旧实现的致命区间：attempt = 62 / 63 / 64
     * 旧代码 {@code interval * (1L << 63)} 为负数 → scheduler 立即执行 → 重连风暴
     */
    @Test
    @DisplayName("attempt 62/63/64 不产生负延迟（旧实现溢出点）")
    void attemptAroundShiftOverflowProducesNoNegativeDelay() {
        ReconnectPolicy policy = new ReconnectPolicy(5, Integer.MAX_VALUE);

        assertTrue(policy.calculateReconnectDelay(62) > 0, "attempt=62 不应产生非正延迟");
        assertTrue(policy.calculateReconnectDelay(63) > 0, "attempt=63 不应产生非正延迟（旧实现此处溢出为负）");
        assertTrue(policy.calculateReconnectDelay(64) > 0, "attempt=64 不应产生非正延迟");
        assertEquals(MAX, policy.calculateReconnectDelay(63));
        assertEquals(MAX, policy.calculateReconnectDelay(64));
    }

    /**
     * 延迟单调不减
     */
    @Test
    @DisplayName("延迟随 attempt 单调不减")
    void delayIsMonotonicallyNonDecreasing() {
        ReconnectPolicy policy = new ReconnectPolicy(1, 100);
        long previous = policy.calculateReconnectDelay(0);
        for (int attempt = 1; attempt <= 20; attempt++) {
            long current = policy.calculateReconnectDelay(attempt);
            assertTrue(current >= previous, "attempt=" + attempt + " 出现延迟回退: " + previous + " -> " + current);
            previous = current;
        }
    }

    /**
     * 负数 attempt 按 0 处理，不抛异常
     */
    @Test
    @DisplayName("负数 attempt 按 0 处理")
    void negativeAttemptIsTreatedAsZero() {
        ReconnectPolicy policy = new ReconnectPolicy(5, 5);
        assertEquals(policy.calculateReconnectDelay(0), policy.calculateReconnectDelay(-1));
        assertEquals(policy.calculateReconnectDelay(0), policy.calculateReconnectDelay(Integer.MIN_VALUE));
    }

    /**
     * 超大 attempt 不会造成长循环（应在毫秒级返回）
     */
    @Test
    @DisplayName("attempt=Integer.MAX_VALUE 立即返回且不挂起")
    void hugeAttemptReturnsQuickly() {
        ReconnectPolicy policy = new ReconnectPolicy(1, Integer.MAX_VALUE);
        long start = System.nanoTime();
        long delay = policy.calculateReconnectDelay(Integer.MAX_VALUE);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000L;

        assertEquals(MAX, delay);
        assertTrue(elapsedMillis < 1000L, "计算耗时应远小于 1 秒，实际 " + elapsedMillis + "ms");
    }

    /**
     * 重试次数语义：maxAttempts = 0 表示不自动重连（与旧行为一致）
     */
    @Test
    @DisplayName("maxAttempts=0 时不允许任何自动重连")
    void zeroMaxAttemptsDisablesAutoReconnect() {
        ReconnectPolicy policy = new ReconnectPolicy(5, 0);
        assertFalse(policy.isAutoReconnectEnabled());
        assertFalse(policy.canAttempt(0));
    }

    /**
     * 负数 maxAttempts 归一化为 0
     */
    @Test
    @DisplayName("负数 maxAttempts 归一化为 0")
    void negativeMaxAttemptsIsNormalizedToZero() {
        ReconnectPolicy policy = new ReconnectPolicy(5, -10);
        assertEquals(0, policy.getMaxAttempts());
        assertFalse(policy.isAutoReconnectEnabled());
    }

    /**
     * 正常重试次数：attempt 从 0 开始，允许 attempt < maxAttempts
     */
    @Test
    @DisplayName("canAttempt 边界正确")
    void canAttemptBoundaryIsCorrect() {
        ReconnectPolicy policy = new ReconnectPolicy(5, 3);
        assertTrue(policy.isAutoReconnectEnabled());
        assertTrue(policy.canAttempt(0));
        assertTrue(policy.canAttempt(1));
        assertTrue(policy.canAttempt(2));
        assertFalse(policy.canAttempt(3));
        assertFalse(policy.canAttempt(4));
    }
}
