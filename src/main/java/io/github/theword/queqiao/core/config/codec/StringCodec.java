package io.github.theword.queqiao.core.config.codec;

import io.github.theword.queqiao.core.config.exception.ConfigValidationException;

/**
 * 字符串编解码器
 *
 * <p><b>null 视为无效配置</b>：非 Optional 配置项不接受 null。
 * 需要"空字符串"请显式写 {@code key: ""}，而不是 {@code key:}。
 * 这条规则统一由 Loader/Runtime 层保证，codec 不再自行决定 null 的语义。
 *
 * @since 0.6.12
 */
public final class StringCodec implements ConfigCodec<String> {

    public static final StringCodec INSTANCE = new StringCodec();

    private StringCodec() {
    }

    @Override
    public String read(String path, Object raw) {
        if (raw instanceof CharSequence) {
            return raw.toString();
        }
        throw new ConfigValidationException(path, "期望 string，实际 " + CodecSupport.describe(raw));
    }

    @Override
    public Object write(String value) {
        return value;
    }

    @Override
    public String typeName() {
        return "string";
    }

    @Override
    public String copy(String value) {
        return value;
    }
}
