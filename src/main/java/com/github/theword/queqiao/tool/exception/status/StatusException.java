package com.github.theword.queqiao.tool.exception.status;

import com.github.theword.queqiao.tool.exception.QueQiaoException;

public class StatusException extends QueQiaoException {
    public StatusException(String message) {
        super(message);
    }

    public StatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
