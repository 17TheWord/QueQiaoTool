package io.github.theword.queqiao.core.exception.status;

import io.github.theword.queqiao.core.exception.QueQiaoException;

public class StatusException extends QueQiaoException {
    public StatusException(String message) {
        super(message);
    }

    public StatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
