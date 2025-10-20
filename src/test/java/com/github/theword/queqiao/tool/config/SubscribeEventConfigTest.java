package com.github.theword.queqiao.tool.config;

import static org.junit.jupiter.api.Assertions.*;

class SubscribeEventConfigTest {

    @org.junit.jupiter.api.Test
    void defaultValues() {
        SubscribeEventConfig config = new SubscribeEventConfig();
        assertTrue(config.isPlayerChat());
        assertTrue(config.isPlayerDeath());
        assertTrue(config.isPlayerJoin());
        assertTrue(config.isPlayerQuit());
        assertTrue(config.isPlayerCommand());
        assertTrue(config.isPlayerAdvancement());
    }

}