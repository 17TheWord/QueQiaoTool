package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.exception.protocol.ResponseStatusException;

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

    public static ResponseEnum fromString(String value) {
        for (ResponseEnum response : ResponseEnum.values()) {
            if (response.value.equalsIgnoreCase(value)) {
                return response;
            }
        }
        throw ResponseStatusException.unknown(value);
    }
}
