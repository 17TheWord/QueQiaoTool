package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.achievement.DisplayModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerChatEventTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private PlayerModel createFakePlayerModel() {
        PlayerModel player = new PlayerModel();
        player.setNickname("TestPlayer");
        player.setUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        player.setAddress("127.0.0.1");
        player.setHealth(20.0);
        player.setMaxHealth(20.0);
        player.setExperienceLevel(5);
        player.setExperienceProgress(0.5);
        player.setTotalExperience(100);
        player.setOp(true);
        player.setWalkSpeed(0.2);
        player.setX(1.0);
        player.setY(64.0);
        player.setZ(1.0);
        return player;
    }

    private void assertPlayerModel(PlayerModel player) {
        assertEquals("TestPlayer", player.getNickname());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), player.getUuid());
        assertEquals("127.0.0.1", player.getAddress());
        assertEquals(20.0, player.getHealth());
        assertEquals(20.0, player.getMaxHealth());
        assertEquals(5, player.getExperienceLevel());
        assertEquals(0.5, player.getExperienceProgress());
        assertEquals(100, player.getTotalExperience());
        assertTrue(player.getOp());
        assertEquals(0.2, player.getWalkSpeed());
        assertEquals(1.0, player.getX());
        assertEquals(64.0, player.getY());
        assertEquals(1.0, player.getZ());
    }

    @Test
    void testPlayerChatEventWithFakeData() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);

        PlayerModel player = createFakePlayerModel();
        String messageId = "msg-001";
        String rawMessage = "<TestPlayer> Hello, world!";
        String message = "Hello, world!";

        PlayerChatEvent event = new PlayerChatEvent(player, messageId, rawMessage, message);

        // 验证PlayerModel
        assertSame(player, event.getPlayer(), "PlayerModel should match");
        assertPlayerModel(event.getPlayer());
        // 验证消息内容
        assertEquals(messageId, event.getMessageId());
        assertEquals(rawMessage, event.getRawMessage());
        assertEquals(message, event.getMessage());
        // 验证事件本身属性
        assertEquals("PlayerChatEvent", event.getEventName());
        assertEquals("message", event.getPostType());
        assertEquals("player_chat", event.getSubType());
        assertTrue(event.getTimestamp() > 0);
        assertNotNull(event.getServerName());
    }
}

