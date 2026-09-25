package com.github.theword.queqiao.tool.config.codec;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

/**
 * 整数编解码器
 *
 * <p><b>严格类型</b>：{@code 8080.0} 不会被接受为整数——
 * 它更可能是用户写错了类型，而不是正常输入。
 *
 * @since 0.6.12
 */
public final class IntegerCodec implements ConfigCodec<Integer> {

    public static final IntegerCodec INSTANCE = new IntegerCodec();

    private IntegerCodec() {
    }

    @Override
    public Integer read(String path, Object raw) {
        if (raw instanceof Integer) {
            return (Integer) raw;
        }
        throw new ConfigValidationException(path, "期望 integer，实际 " + CodecSupport.describe(raw));
    }

    @Override
    public Object write(Integer value) {
        return value;
    }

    @Override
    public String typeName() {
        return "integer";
    }

    @Override
    public Integer copy(Integer value) {
        return value;
    }
}
