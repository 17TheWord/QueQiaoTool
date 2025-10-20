package com.github.theword.queqiao.tool.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolTest {

    @Test
    void testIsRegisterOrLoginCommand() {
        assertEquals("", Tool.isRegisterOrLoginCommand("/login test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("login test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("/register test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("register test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("/l test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("l test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("/reg test"));
        assertEquals("", Tool.isRegisterOrLoginCommand("reg test"));
        assertEquals("other test", Tool.isRegisterOrLoginCommand("other test"));
        assertEquals("other test", Tool.isRegisterOrLoginCommand("/other test"));
    }

}

