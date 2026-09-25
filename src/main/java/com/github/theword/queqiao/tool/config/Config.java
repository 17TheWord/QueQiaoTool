package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.io.ConfigDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 配置运行时值存储
 *
 * <p><b>职责边界</b>：本类<b>只</b>负责把 Schema 中定义的
 * {@link ConfigKey} 变成一个可靠、类型安全、隔离良好的 Runtime Value Store。
 * <b>不</b>包含 YAML 解析、文件读写、同步、命令、热加载——那些分别属于
 * {@code ConfigLoader}、{@code ConfigWriter}、{@code ConfigSynchronizer} 与命令层。
 *
 * <pre>
 * Config = Runtime Value Store   ← 本类
 * ConfigLoader  = YAML → Runtime
 * ConfigWriter  = Runtime → YAML
 * ConfigRegistry= Schema
 * </pre>
 *
 * <p><b>关键不变量</b>：
 * <ul>
 *     <li><b>不暴露可变值引用</b>：{@code get} 与 {@code set} 都经 {@link ConfigCodec#copy(Object)}，
 *         外部无法通过 {@code config.get(listKey).add(...)} 绕过校验直接改内部状态；</li>
 *     <li><b>{@code set} 失败不污染旧值</b>：先解析校验，全部通过后才写入；</li>
 *     <li><b>{@code load} 原子替换</b>：全部转换校验成功后才一次性替换状态，失败时保持原状态；</li>
 *     <li><b>Key 归属校验</b>：{@code ConfigKey} 以对象身份标识，使用其它 Registry 的 Key 会抛异常；</li>
 *     <li><b>不修改 ConfigKey</b>：Schema 不可变，运行时状态可变，二者严格分离；</li>
 *     <li><b>{@code set} 不落盘</b>：持久化由 Phase 5 的 Writer 负责，避免批量修改产生多次 I/O。</li>
 * </ul>
 *
 * <p><b>并发</b>：状态是不可变快照，持有在 {@link AtomicReference} 中。
 * {@code get} 无锁读取；{@code set}/{@code reset}/{@code load} 用 CAS 替换整份状态，
 * 因此读操作不会看到"半加载"状态。
 *
 * @since 0.6.12
 */
public final class Config {

    private final ConfigRegistry registry;
    private final AtomicReference<State> stateRef;

    /**
     * 构造运行时值存储
     *
     * @param registry Schema 注册中心
     */
    public Config(ConfigRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("ConfigRegistry 不能为 null");
        }
        this.registry = registry;
        this.stateRef = new AtomicReference<>(new State(Collections.<ConfigKey<?>, ValueEntry>emptyMap()));
    }

    // ------------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------------

    /**
     * 读取生效值
     *
     * <p>字段缺失时返回默认值（不是 null）。可变类型返回<b>副本</b>，
     * 修改返回值不会影响内部状态。
     *
     * @param key 配置项
     * @param <T> 值类型
     * @return 生效值副本
     * @throws ConfigValidationException key 不属于本 Runtime 的 Registry
     */
    public <T> T get(ConfigKey<T> key) {
        requireRegistered(key);
        ValueEntry entry = stateRef.get().entries.get(key);
        if (entry == null) {
            return key.defaultValue();
        }
        return key.getCodec().copy(cast(entry.value));
    }

    /**
     * 判断用户配置中是否<b>显式存在</b>该字段
     *
     * <p>与 {@link #get(ConfigKey)} 语义严格区分：缺失字段的 {@code get} 返回默认值，
     * 但 {@code contains} 返回 false。
     *
     * @param key 配置项
     * @return true 表示值来源于用户配置
     */
    public boolean contains(ConfigKey<?> key) {
        requireRegistered(key);
        ValueEntry entry = stateRef.get().entries.get(key);
        return entry != null && entry.source == ConfigValueSource.USER;
    }

    /**
     * 判断当前值是否<b>来源于默认值</b>
     *
     * <p><b>不是</b>简单的 {@code value.equals(defaultValue)}：
     * 用户显式写了与默认值相同的值，仍然属于 {@code USER}，本方法返回 false。
     *
     * @param key 配置项
     * @return true 表示值来源于默认值
     */
    public boolean isDefault(ConfigKey<?> key) {
        requireRegistered(key);
        ValueEntry entry = stateRef.get().entries.get(key);
        return entry == null || entry.source == ConfigValueSource.DEFAULT;
    }

    // ------------------------------------------------------------------
    // 修改
    // ------------------------------------------------------------------

    /**
     * 设置运行时值
     *
     * <p>流程：<b>归属校验 → codec 解析（类型一致性）→ validator → copy → 写入</b>。
     * 任何一步失败都抛 {@link ConfigValidationException}，且<b>旧值保持不变</b>。
     *
     * <p><b>不会自动落盘</b>——持久化由调用方显式触发（未来命令层 {@code set → save}）。
     *
     * @param key   配置项
     * @param value 新值
     * @param <T>   值类型
     * @throws ConfigValidationException 类型不符或校验不通过
     */
    public <T> void set(ConfigKey<T> key, T value) {
        requireRegistered(key);
        T normalized = normalizeAndValidate(key, value);
        T stored = key.getCodec().copy(normalized);
        update(entries -> entries.put(key, new ValueEntry(stored, ConfigValueSource.USER)));
    }

    /**
     * 恢复为默认值
     *
     * <p>结果与"用户配置中不存在该字段"一致：{@code get} 返回默认值副本，
     * {@code contains} 为 false，{@code isDefault} 为 true。
     * 每次 reset 都经 codec 复制，因此多次 reset 得到的可变默认值互不影响。
     *
     * @param key 配置项
     * @param <T> 值类型
     */
    public <T> void reset(ConfigKey<T> key) {
        requireRegistered(key);
        T defaultValue = key.defaultValue();
        update(entries -> entries.put(key, new ValueEntry(defaultValue, ConfigValueSource.DEFAULT)));
    }

    // ------------------------------------------------------------------
    // 批量加载（原子）
    // ------------------------------------------------------------------

    /**
     * 用一份<b>已解析</b>的配置文档原子替换运行时状态
     *
     * <p><b>YAML 解析不在这里</b>：由 {@code ConfigFileReader} 负责把文件解析成
     * {@code Map<String, Object>}（或 {@link ConfigDocument}）后调用本方法。
     *
     * <p><b>原子性</b>：先对全部已注册的 Key 完成"取值 → codec 解析 → validator"，
     * 全部成功后才一次性替换状态；任何一项失败都会抛异常并<b>保持 load 之前的状态</b>，
     * 绝不会出现"一半新一半旧"。
     *
     * <p><b>缺失即默认</b>：文档中没有的字段不算错误，记为
     * {@link ConfigValueSource#DEFAULT}；只有"存在但类型错误 / 校验失败"才是配置错误。
     *
     * @param document 已解析的配置文档，可为 null（等价于空文档）
     * @throws ConfigValidationException 存在类型错误或校验失败的字段
     */
    public void load(Map<String, Object> document) {
        load(new ConfigDocument(document));
    }

    /**
     * 用一份已解析的配置文档原子替换运行时状态
     *
     * <p>与 {@link #load(Map)} 等价，但走的是统一的路径解析入口（{@link ConfigDocument}），
     * 避免出现第二套路径遍历逻辑。
     *
     * @param document 已解析的配置文档，可为 null（等价于空文档）
     * @throws ConfigValidationException 存在类型错误或校验失败的字段
     */
    public void load(ConfigDocument document) {
        ConfigDocument source = document == null ? ConfigDocument.empty() : document;

        Map<ConfigKey<?>, ValueEntry> next = new LinkedHashMap<>();
        for (ConfigKey<?> key : registry.snapshot().getKeys()) {
            next.put(key, buildEntry(key, source));
        }

        // 全部成功后才替换——上面任何一步抛异常都不会走到这里
        stateRef.set(new State(next));
    }

    /**
     * @return 已注册配置项数量
     */
    public int size() {
        return registry.size();
    }

    /**
     * 取当前状态的<b>不可变快照</b>
     *
     * <p>供 Writer 等长操作使用：它们只处理快照，因此不会被并发的 {@code set}/{@code load} 影响，
     * 也不会长时间持锁。
     *
     * @return 运行时快照
     */
    public ConfigSnapshot snapshot() {
        State state = stateRef.get();
        Map<ConfigKey<?>, Object> values = new LinkedHashMap<>();
        Set<ConfigKey<?>> userKeys = new LinkedHashSet<>();
        for (Map.Entry<ConfigKey<?>, ValueEntry> entry : state.entries.entrySet()) {
            values.put(entry.getKey(), entry.getValue().value);
            if (entry.getValue().source == ConfigValueSource.USER) {
                userKeys.add(entry.getKey());
            }
        }
        return new ConfigSnapshot(values, userKeys);
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    /**
     * 为一个 Key 构建值条目
     *
     * <p><b>null 的统一语义</b>：字段<b>缺失</b> → 默认值；字段<b>存在但值为 null</b> →
     * 视为无效配置（非 Optional 配置项不接受 null）。该规则集中在这里，
     * 不让各 codec 各自决定（避免出现 BooleanCodec 认为 null=false、
     * StringCodec 认为 null="" 这类隐式行为）。
     */
    private <T> ValueEntry buildEntry(ConfigKey<T> key, ConfigDocument document) {
        if (!document.has(key.getPath())) {
            return new ValueEntry(key.defaultValue(), ConfigValueSource.DEFAULT);
        }

        Object raw = document.get(key.getPath());
        if (raw == null) {
            throw new ConfigValidationException(
                    key.getPath(), "值不能为 null（非 Optional 配置项不接受 null）");
        }

        T parsed = key.getCodec().read(key.getPath(), raw);
        T normalized = normalizeAndValidate(key, parsed);
        return new ValueEntry(key.getCodec().copy(normalized), ConfigValueSource.USER);
    }

    /**
     * codec 解析（类型一致性）+ validator 校验；任何失败都抛异常且不改状态
     */
    private <T> T normalizeAndValidate(ConfigKey<T> key, T value) {
        T normalized = key.getCodec().read(key.getPath(), value);
        key.validate(normalized);
        return normalized;
    }

    /**
     * 校验 Key 是否属于本 Runtime 使用的 Registry
     *
     * <p>{@code ConfigKey} 以<b>对象身份</b>标识，因此这里用 {@code ==} 比较：
     * 其它 Registry 中 path 相同的 Key 会被拒绝，避免跨 Runtime 污染。
     */
    private void requireRegistered(ConfigKey<?> key) {
        if (key == null) {
            throw new IllegalArgumentException("配置项不能为 null");
        }
        if (registry.findByPath(key.getPath()) != key) {
            throw new ConfigValidationException(key.getPath(), "该配置项不属于当前 Config 的 Registry");
        }
    }

    /**
     * CAS 替换状态（无锁）
     */
    private void update(Consumer<Map<ConfigKey<?>, ValueEntry>> mutator) {
        while (true) {
            State current = stateRef.get();
            Map<ConfigKey<?>, ValueEntry> next = new LinkedHashMap<>(current.entries);
            mutator.accept(next);
            if (stateRef.compareAndSet(current, new State(next))) {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    /**
     * 供测试与调试使用的值快照描述
     *
     * @return 形如 {@code websocket_server.port=8080(USER)} 的列表
     */
    public List<String> describeEntries() {
        List<String> result = new ArrayList<>();
        for (Map.Entry<ConfigKey<?>, ValueEntry> entry : stateRef.get().entries.entrySet()) {
            result.add(entry.getKey().getPath() + "=" + entry.getValue().value + "(" + entry.getValue().source + ")");
        }
        return result;
    }

    /**
     * 不可变运行时状态
     */
    private static final class State {

        private final Map<ConfigKey<?>, ValueEntry> entries;

        private State(Map<ConfigKey<?>, ValueEntry> entries) {
            this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
        }
    }

    /**
     * 单个配置项的值条目
     */
    private static final class ValueEntry {

        private final Object value;
        private final ConfigValueSource source;

        private ValueEntry(Object value, ConfigValueSource source) {
            this.value = value;
            this.source = source;
        }
    }
}
