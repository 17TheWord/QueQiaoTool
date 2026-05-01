package com.github.theword.queqiao.tool.exception.protocol;

import com.github.theword.queqiao.tool.exception.QueQiaoToolException;

/**
 * 响应状态解析异常。
 */
public class ResponseStatusException extends QueQiaoToolException {
    private ResponseStatusException(String message) {
        super(message);
    }

    private static ResponseStatusException of(String message) {
        return new ResponseStatusException(message);
    }

    public static ResponseStatusException unknown(String value) {
        return of("Unknown response status: " + value);
    }
}
