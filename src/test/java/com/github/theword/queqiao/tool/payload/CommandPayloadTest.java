package com.github.theword.queqiao.tool.payload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandPayloadTest {

    @Test
    void testGetAndSetCommand() {
        CommandPayload payload = new CommandPayload();
        String testCommand = "/say Hello, World!";
        payload.setCommand(testCommand);
        assertEquals(testCommand, payload.getCommand());
    }

}