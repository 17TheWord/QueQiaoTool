package com.github.theword.queqiao.tool.exception.protocol;

import com.github.theword.queqiao.tool.exception.QueQiaoToolException;

/**
 * 协议 API 处理器注册异常。
 */
public class ProtocolHandlerRegistryException extends QueQiaoToolException {
    private final Reason reason;

    private ProtocolHandlerRegistryException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private static ProtocolHandlerRegistryException of(Reason reason, String message) {
        return new ProtocolHandlerRegistryException(reason, message);
    }

    public static ProtocolHandlerRegistryException apiBlank() {
        return of(Reason.API_BLANK, "Protocol handler api must not be null or empty");
    }

    public static ProtocolHandlerRegistryException handlerNull(String api) {
        return of(Reason.HANDLER_NULL, "Protocol handler for api '" + api + "' must not be null");
    }

    public static ProtocolHandlerRegistryException duplicated(String api) {
        return of(Reason.DUPLICATED, "Protocol handler for api '" + api + "' is already registered");
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        API_BLANK,
        HANDLER_NULL,
        DUPLICATED
    }
}
