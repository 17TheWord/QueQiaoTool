package com.github.theword.queqiao.tool.exception;

/**
 * QueQiaoTool 基础异常。
 */
public class QueQiaoToolException extends RuntimeException {
    public QueQiaoToolException(String message) {
        super(message);
    }

    public QueQiaoToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
