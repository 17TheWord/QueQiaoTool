package io.github.theword.queqiao.core.response;

import io.github.theword.queqiao.core.exception.response.ResponseException;

public enum ResponseEnum {
    SUCCESS("SUCCESS"), FAILED("FAILED");

    private final String value;

    ResponseEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ResponseEnum fromString(String value) throws ResponseException {
        for (ResponseEnum response : ResponseEnum.values()) {
            if (response.value.equalsIgnoreCase(value)) {
                return response;
            }
        }
        throw ResponseException.unknownValue(value);
    }
}
