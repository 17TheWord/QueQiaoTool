package io.github.theword.queqiao.core.payload;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessagePayloadTest {

    @Test
    void testGetAndSetMessage() {
        MessagePayload payload = new MessagePayload();
        assertNull(payload.getMessage(), "Initial message should be null");

        JsonElement sampleMessage = new JsonPrimitive("Test Message");

        payload.setMessage(sampleMessage);
        assertEquals(sampleMessage, payload.getMessage(), "The message should match the set value");
    }

}