package io.github.theword.queqiao.core.exception.response;

import io.github.theword.queqiao.core.exception.QueQiaoException;

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
