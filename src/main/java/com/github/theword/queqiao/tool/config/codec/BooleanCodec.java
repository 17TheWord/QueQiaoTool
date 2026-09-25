package com.github.theword.queqiao.tool.config.codec;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

/**
 * 布尔值编解码器
 *
 * @since 0.6.12
 */
public final class BooleanCodec implements ConfigCodec<Boolean> {

    public static final BooleanCodec INSTANCE = new BooleanCodec();

    private BooleanCodec() {
    }

    @Override
    public Boolean read(String path, Object raw) {
        if (raw instanceof Boolean) {
            return (Boolean) raw;
        }
        throw new ConfigValidationException(path, "期望 boolean，实际 " + CodecSupport.describe(raw));
    }

    @Override
    public Object write(Boolean value) {
        return value;
    }

    @Override
    public String typeName() {
        return "boolean";
    }

    @Override
    public Boolean copy(Boolean value) {
        return value;
    }
}
