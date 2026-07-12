package com.github.theword.queqiao.tool.exception.protocol;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.QueQiaoException;

public class ProtocolException extends QueQiaoException {
    private final int code;
    private final Object data;

    public ProtocolException(int code, String message) {
        this(code, message, null);
    }

    public ProtocolException(int code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public static ProtocolException badRequest(String message) {
        return new ProtocolException(ProtocolConstants.Status.BAD_REQUEST, message);
    }

    public static ProtocolException badRequest(String message, Object data) {
        return new ProtocolException(ProtocolConstants.Status.BAD_REQUEST, message, data);
    }

    public static ProtocolException internalError(String message) {
        return new ProtocolException(ProtocolConstants.Status.INTERNAL_ERROR, message);
    }
}
