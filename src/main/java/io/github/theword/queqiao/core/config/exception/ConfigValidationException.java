package io.github.theword.queqiao.core.config.exception;

/**
 * 显式识别出的<b>用户配置错误</b>
 *
 * <p>与"未预期的内部异常"严格区分：
 * <ul>
 *     <li>本异常代表<b>用户配置本身不合法</b>（类型错误、数值越界等），
 *         由解析与校验阶段主动识别并抛出，可安全地回退默认值；</li>
 *     <li>其它 {@link RuntimeException} 代表<b>程序缺陷</b>，
 *         必须 ERROR + 堆栈并向上抛出，<b>不得</b>被静默转换成默认配置
 *         ——否则真实缺陷会被伪装成"配置问题"而永远查不出来。</li>
 * </ul>
 *
 * @since 0.6.12
 */
public class ConfigValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 出错字段的点分路径，如 {@code websocket_server.port}
     */
    private final String fieldPath;

    public ConfigValidationException(String fieldPath, String message) {
        super(fieldPath + ": " + message);
        this.fieldPath = fieldPath;
    }

    public ConfigValidationException(String fieldPath, String message, Throwable cause) {
        super(fieldPath + ": " + message, cause);
        this.fieldPath = fieldPath;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
