package com.github.theword.queqiao.tool.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseEnumTest {

    @Test
    void testFromString() {
        assertEquals(ResponseEnum.SUCCESS, ResponseEnum.fromString("SUCCESS"));
        assertEquals(ResponseEnum.FAILED, ResponseEnum.fromString("FAILED"));
        assertEquals(ResponseEnum.SUCCESS, ResponseEnum.fromString("success"));
        assertEquals(ResponseEnum.FAILED, ResponseEnum.fromString("failed"));
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ResponseEnum.fromString("UNKNOWN");
        });
        assertEquals("Unknown value: UNKNOWN", exception.getMessage());
    }

    @Test
    void testEnumValues() {
        assertEquals("SUCCESS", ResponseEnum.SUCCESS.getValue());
        assertEquals("FAILED", ResponseEnum.FAILED.getValue());
        assertEquals("SUCCESS", ResponseEnum.SUCCESS.toString());
        assertEquals("FAILED", ResponseEnum.FAILED.toString());
    }

}