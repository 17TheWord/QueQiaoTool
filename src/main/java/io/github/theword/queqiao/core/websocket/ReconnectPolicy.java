package io.github.theword.queqiao.core.websocket;

/**
 * 重连退避策略
 *
 * <p>纯逻辑组件：不依赖网络、线程、全局状态，可独立单元测试。
 *
 * <p>由 {@link WsClient} 在计算下一次自动重连延迟时使用。
 *
 * <p>契约（不变量）：
 * <ul>
 *     <li>返回的延迟始终满足 {@code MIN_RECONNECT_DELAY_SECONDS <= delay <= MAX_RECONNECT_DELAY_SECONDS}</li>
 *     <li>绝不产生负数延迟（负数交给 scheduler 会被当作立即执行，形成重连风暴）</li>
 *     <li>计算过程不使用位移，避免 {@code 1L << attempt} 在 attempt >= 63 时溢出为负</li>
 * </ul>
 *
 * @since 0.6.11
 */
public final class ReconnectPolicy {

    /**
     * 重连延迟上限（秒）
     */
    public static final long MAX_RECONNECT_DELAY_SECONDS = 60L;

    /**
     * 重连延迟下限（秒）
     *
     * <p>当配置的间隔非法（负数或 0）时，回退到该值。
     */
    public static final long MIN_RECONNECT_DELAY_SECONDS = 1L;

    /**
     * 基础重连间隔（秒），已归一化到 [MIN, MAX]
     */
    private final long baseIntervalSeconds;

    /**
     * 最大自动重连次数，已归一化到 >= 0
     *
     * <p>等于 0 表示不进行自动重连（与旧行为一致）。
     */
    private final int maxAttempts;

    /**
     * 构造重连策略
     *
     * @param baseIntervalSeconds 配置中的基础重连间隔（秒），非法值会被归一化
     * @param maxAttempts         配置中的最大重连次数，负数会被归一化为 0
     */
    public ReconnectPolicy(int baseIntervalSeconds, int maxAttempts) {
        this.baseIntervalSeconds = normalizeBaseInterval(baseIntervalSeconds);
        this.maxAttempts = Math.max(maxAttempts, 0);
    }

    /**
     * 归一化基础重连间隔
     *
     * @param baseIntervalSeconds 原始间隔（秒）
     * @return 落在 [MIN_RECONNECT_DELAY_SECONDS, MAX_RECONNECT_DELAY_SECONDS] 内的间隔
     */
    public static long normalizeBaseInterval(int baseIntervalSeconds) {
        if (baseIntervalSeconds <= 0) {
            return MIN_RECONNECT_DELAY_SECONDS;
        }
        return Math.min(baseIntervalSeconds, MAX_RECONNECT_DELAY_SECONDS);
    }

    /**
     * 计算第 attempt 次重连的延迟
     *
     * <p>退避方式为指数增长并封顶：{@code min(base * 2^attempt, MAX)}。
     * 由于每步乘法前都已受 {@code MAX} 约束（{@code delay < MAX} 才继续翻倍），
     * 因此不存在乘法溢出；循环次数上界为 {@code log2(MAX / base)}，
     * 传入 {@code Integer.MAX_VALUE} 也不会退化。
     *
     * @param attempt 连续失败次数，从 0 开始；负数按 0 处理
     * @return 延迟秒数，恒落在 [MIN, MAX] 内
     */
    public long calculateReconnectDelay(int attempt) {
        long delay = baseIntervalSeconds;
        for (int i = 0; i < attempt && delay < MAX_RECONNECT_DELAY_SECONDS; i++) {
            delay = Math.min(delay * 2L, MAX_RECONNECT_DELAY_SECONDS);
        }
        if (delay < MIN_RECONNECT_DELAY_SECONDS) {
            return MIN_RECONNECT_DELAY_SECONDS;
        }
        return Math.min(delay, MAX_RECONNECT_DELAY_SECONDS);
    }

    /**
     * 判断是否还允许继续自动重连
     *
     * @param attempt 当前已连续失败的次数
     * @return true 表示允许再安排一次重连
     */
    public boolean canAttempt(int attempt) {
        return attempt < maxAttempts;
    }

    /**
     * 是否启用了自动重连
     *
     * @return {@code maxAttempts > 0} 时为 true
     */
    public boolean isAutoReconnectEnabled() {
        return maxAttempts > 0;
    }

    /**
     * 获取归一化后的基础重连间隔（秒）
     *
     * @return 基础间隔
     */
    public long getBaseIntervalSeconds() {
        return baseIntervalSeconds;
    }

    /**
     * 获取归一化后的最大自动重连次数
     *
     * @return 最大次数，0 表示不自动重连
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
}
