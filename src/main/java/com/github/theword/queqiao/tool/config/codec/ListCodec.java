package com.github.theword.queqiao.tool.config.codec;

import com.github.theword.queqiao.tool.config.ConfigCodec;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 字符串列表编解码器
 *
 * <p>{@link #copy(List)} 返回<b>新列表</b>——这是"默认值不在多个 {@code Config} 实例之间共享
 * 同一个可变对象"的保证。
 *
 * <p><b>null 视为无效配置</b>：非 Optional 配置项不接受 null；需要空列表请显式写 {@code []}。
 *
 * @since 0.6.12
 */
public final class ListCodec implements ConfigCodec<List<String>> {

    public static final ListCodec INSTANCE = new ListCodec();

    private ListCodec() {
    }

    @Override
    public List<String> read(String path, Object raw) {
        if (!(raw instanceof List)) {
            throw new ConfigValidationException(path, "期望 list，实际 " + CodecSupport.describe(raw));
        }

        List<String> result = new ArrayList<>();
        for (Object element : (List<?>) raw) {
            if (element == null) {
                continue;
            }
            if (!(element instanceof CharSequence)) {
                throw new ConfigValidationException(
                        path, "列表元素期望 string，实际 " + CodecSupport.describe(element));
            }
            result.add(element.toString());
        }
        return result;
    }

    @Override
    public Object write(List<String> value) {
        return value == null ? Collections.emptyList() : new ArrayList<>(value);
    }

    @Override
    public String typeName() {
        return "list of string";
    }

    @Override
    public List<String> copy(List<String> value) {
        return value == null ? new ArrayList<String>() : new ArrayList<>(value);
    }
}
