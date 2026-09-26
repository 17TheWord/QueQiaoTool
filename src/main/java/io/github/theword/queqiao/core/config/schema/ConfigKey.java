package io.github.theword.queqiao.core.config.schema;

import io.github.theword.queqiao.core.config.codec.ConfigCodec;
import io.github.theword.queqiao.core.config.exception.ConfigValidationException;
import io.github.theword.queqiao.core.config.validation.ConfigValidator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 配置项定义——配置系统的<b>最小单元</b>
 *
 * <p>一个 {@code ConfigKey<T>} 自带：<b>path、codec、默认值、validator、注释</b>。
 * 它是配置的<b>唯一来源</b>：类型安全由泛型保证，取值必须写
 * {@code config.get(ConfigKeys.WebSocket.PORT)}，而不是字符串查找。
 *
 * <p><b>不可变</b>：注册后不得修改任何字段，否则树与运行时容易不一致。
 *
 * <p><b>equals/hashCode 使用对象 identity</b>（不重写）：不同 Runtime 可以有 path 相同的
 * 两个不同 {@code ConfigKey} 对象，不应互相混用。同一 Registry 内的 path 唯一性由
 * {@link ConfigRegistry} 保证。
 *
 * @param <T> 配置值类型
 * @since 0.6.12
 */
public final class ConfigKey<T> extends ConfigNode {

    private final ConfigCodec<T> codec;
    private final Supplier<? extends T> defaultValueSupplier;
    private final ConfigValidator<? super T> validator;

    private ConfigKey(
            String path,
            List<String> commentLines,
            ConfigCodec<T> codec,
            Supplier<? extends T> defaultValueSupplier,
            ConfigValidator<? super T> validator) {
        super(path, commentLines);
        this.codec = codec;
        this.defaultValueSupplier = defaultValueSupplier;
        this.validator = validator;
    }

    /**
     * 创建构建器
     *
     * @param path  完整点分路径（必须与现有 YAML 路径保持兼容）
     * @param codec 编解码器
     * @param <T>   值类型
     * @return 构建器
     */
    public static <T> Builder<T> builder(String path, ConfigCodec<T> codec) {
        return new Builder<>(path, codec);
    }

    /**
     * 取默认值
     *
     * <p><b>每次调用都返回副本</b>（由 codec 的 {@code copy} 保证）——
     * 因此多个 {@code Config} 实例不会共享同一个可变默认对象。
     *
     * @return 默认值副本
     */
    public T defaultValue() {
        T value = codec.read(getPath(), defaultValueSupplier.get());
        validate(value);
        return codec.copy(value);
    }

    public ConfigCodec<T> getCodec() {
        return codec;
    }

    public boolean hasValidator() {
        return validator != null;
    }

    /**
     * 解析原始值并校验
     *
     * @param raw YAML 原始值
     * @return 解析后的值
     * @throws ConfigValidationException 类型不符或校验不通过
     */
    public T parse(Object raw) {
        T value = codec.read(getPath(), raw);
        validate(value);
        return value;
    }

    /**
     * 校验一个已解析的值
     *
     * @param value 值
     * @throws ConfigValidationException 校验不通过
     */
    public void validate(T value) {
        if (validator != null) {
            validator.validate(getPath(), value);
        }
    }

    /**
     * {@code ConfigKey} 构建器
     *
     * @param <T> 值类型
     */
    public static final class Builder<T> {

        private final String path;
        private final ConfigCodec<T> codec;
        private Supplier<? extends T> defaultValueSupplier;
        private ConfigValidator<? super T> validator;
        private List<String> commentLines = Collections.emptyList();

        private Builder(String path, ConfigCodec<T> codec) {
            this.path = path;
            this.codec = Objects.requireNonNull(codec, "codec");
        }

        /**
         * 指定默认值
         *
         * <p>可变类型（List/Map 等）也安全：读取默认值时会经 codec 复制。
         * 若默认值构造代价较高，用 {@link #defaultValueSupplier(Supplier)}。
         *
         * @param value 默认值
         * @return 构建器
         */
        public Builder<T> defaultValue(T value) {
            this.defaultValueSupplier = () -> value;
            return this;
        }

        /**
         * 用 Supplier 指定默认值（适合可变类型，保证每次都新建）
         *
         * @param supplier 默认值提供者
         * @return 构建器
         */
        public Builder<T> defaultValueSupplier(Supplier<? extends T> supplier) {
            this.defaultValueSupplier = Objects.requireNonNull(supplier, "supplier");
            return this;
        }

        /**
         * 指定校验器
         *
         * @param validator 校验器
         * @return 构建器
         */
        public Builder<T> validator(ConfigValidator<? super T> validator) {
            this.validator = validator;
            return this;
        }

        /**
         * 指定注释行（会写入生成的 config.yml）
         *
         * @param commentLines 注释行
         * @return 构建器
         */
        public Builder<T> comment(String... commentLines) {
            this.commentLines = lines(commentLines);
            return this;
        }

        /**
         * 构建
         *
         * @return 配置项定义
         */
        public ConfigKey<T> build() {
            if (path == null || path.trim().isEmpty()) {
                throw new IllegalArgumentException("配置项的路径不能为空");
            }
            if (defaultValueSupplier == null) {
                throw new IllegalStateException("必须为 " + path + " 指定默认值");
            }
            ConfigKey<T> key = new ConfigKey<>(path, commentLines, codec, defaultValueSupplier, validator);
            // Schema 中的默认值是程序定义，不应静默回退；在注册前暴露错误路径。
            key.defaultValue();
            return key;
        }
    }
}
