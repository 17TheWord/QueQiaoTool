package io.github.theword.queqiao.core.config.codec;

/**
 * 编解码器的公共辅助
 *
 * @since 0.6.12
 */
final class CodecSupport {

    private CodecSupport() {
    }

    /**
     * 描述一个原始值的类型与内容，用于报错
     *
     * @param raw 原始值
     * @return 形如 {@code integer(8080)} 或 {@code 缺失} 的描述
     */
    static String describe(Object raw) {
        return raw == null ? "缺失" : raw.getClass().getSimpleName() + "(" + raw + ")";
    }
}
