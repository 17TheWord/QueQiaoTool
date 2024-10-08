package com.github.theword.queqiao.tool.response;

import lombok.Getter;

@Getter
public enum ResponseEnum {
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    ResponseEnum(String value) {
        this.value = value;
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
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
