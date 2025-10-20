package com.github.theword.queqiao.tool.response;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.github.theword.queqiao.tool.event.base.BasePlayer;

import java.util.UUID;

class PrivateMessageResponseTest {

    @Test
    void testDefaultConstructorAndSetters() {
        PrivateMessageResponse resp = new PrivateMessageResponse();
        assertNull(resp.getPlayer());
        assertNull(resp.getMessage());
        BasePlayer player = new BasePlayer("Steve", UUID.randomUUID());
        resp.setPlayer(player);
        resp.setMessage("Hello");
        assertEquals(player, resp.getPlayer());
        assertEquals("Hello", resp.getMessage());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        BasePlayer player = new BasePlayer("Alex", UUID.randomUUID());
        PrivateMessageResponse resp = new PrivateMessageResponse(player, "Hi");
        assertEquals(player, resp.getPlayer());
        assertEquals("Hi", resp.getMessage());
    }

    @Test
    void testOfStaticMethod() {
        BasePlayer player = new BasePlayer("Test", UUID.randomUUID());
        PrivateMessageResponse resp = PrivateMessageResponse.of(player, "msg");
        assertEquals(player, resp.getPlayer());
        assertEquals("msg", resp.getMessage());
    }

    @Test
    void testPlayerNotFound() {
        PrivateMessageResponse resp = PrivateMessageResponse.playerNotFound();
        assertNull(resp.getPlayer());
        assertEquals("Target player not found.", resp.getMessage());
    }

    @Test
    void testPlayerNotOnline() {
        PrivateMessageResponse resp = PrivateMessageResponse.playerNotOnline();
        assertNull(resp.getPlayer());
        assertEquals("Target player is not online.", resp.getMessage());
    }

    @Test
    void testPlayerIsNull() {
        PrivateMessageResponse resp = PrivateMessageResponse.playerIsNull();
        assertNull(resp.getPlayer());
        assertEquals("Target player is null.", resp.getMessage());
    }
}