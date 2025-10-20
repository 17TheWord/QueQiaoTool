package com.github.theword.queqiao.tool.payload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BasePayloadTest {

    @Test
    void testGetAndSetApi() {
        BasePayload payload = new BasePayload();
        assertNull(payload.getApi(), "Initial API should be null");

        String sampleApi = "testApi";
        payload.setApi(sampleApi);
        assertEquals(sampleApi, payload.getApi(), "The API should match the set value");
    }

}