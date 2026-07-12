package com.github.theword.queqiao.tool.exception.response;

import com.github.theword.queqiao.tool.exception.QueQiaoException;

public class ResponseException extends QueQiaoException {
    public ResponseException(String message) {
        super(message);
    }

    public ResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResponseException unknownValue(String value) {
        return new ResponseException("Unknown value: " + value);
    }
}
