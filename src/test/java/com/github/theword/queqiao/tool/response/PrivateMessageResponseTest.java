package com.github.theword.queqiao.tool.response;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class PrivateMessageResponseTest {

    @Test
    void testDefaultConstructorAndSetters() {
        PrivateMessageResponse resp = new PrivateMessageResponse();
        assertNull(resp.getPlayer());
        assertNull(resp.getMessage());
        PlayerModel playerModel = new PlayerModel("Steve", UUID.randomUUID());
        resp.setPlayer(playerModel);
        resp.setMessage("Hello");
        assertEquals(playerModel, resp.getPlayer());
        assertEquals("Hello", resp.getMessage());
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        PlayerModel playerModel = new PlayerModel("Alex", UUID.randomUUID());
        PrivateMessageResponse resp = new PrivateMessageResponse(playerModel, "Hi");
        assertEquals(playerModel, resp.getPlayer());
        assertEquals("Hi", resp.getMessage());
    }

    @Test
    void testOfStaticMethod() {
        PlayerModel playerModel = new PlayerModel("Test", UUID.randomUUID());
        PrivateMessageResponse resp = PrivateMessageResponse.of(playerModel, "msg");
        assertEquals(playerModel, resp.getPlayer());
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