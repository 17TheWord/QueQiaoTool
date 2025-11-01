package com.github.theword.queqiao.tool.payload;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrivateMessagePayloadTest {

    @Test
    void testGetAndSetUuid() {
        PrivateMessagePayload payload = new PrivateMessagePayload();
        assertNull(payload.getUuid(), "Initial UUID should be null");

        UUID sampleUuid = UUID.randomUUID();
        payload.setUuid(sampleUuid);
        assertEquals(sampleUuid, payload.getUuid(), "The UUID should match the set value");

        payload.setMessage(new JsonPrimitive("Test Message"));
        assertEquals("Test Message", payload.getMessage().getAsString(), "The message should match the set value");
    }

}