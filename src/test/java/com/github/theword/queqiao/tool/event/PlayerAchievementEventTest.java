package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.achievement.AchievementModel;
import com.github.theword.queqiao.tool.event.model.achievement.DisplayModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAchievementEventTest {

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

    private DisplayModel createFakeDisplayModel() {
        DisplayModel display = new DisplayModel();
        display.setAnnounceChat(true);
        display.setBackground("background/path");
        display.setDescription("Test achievement description");
        display.setFrame("goal");
        display.setHidden(false);
        display.setIcon("minecraft:diamond");
        display.setShowToast(true);
        display.setTitle("Test Achievement");
        display.setX(1.5);
        display.setY(2.5);
        return display;
    }

    private AchievementModel createFakeAchievementModel(DisplayModel display) {
        AchievementModel achievement = new AchievementModel();
        achievement.setDisplay(display);
        return achievement;
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

    private void assertDisplayModel(DisplayModel display) {
        assertNotNull(display);
        assertTrue(display.getAnnounceChat());
        assertEquals("background/path", display.getBackground());
        assertEquals("Test achievement description", display.getDescription());
        assertEquals("goal", display.getFrame());
        assertFalse(display.getHidden());
        assertEquals("minecraft:diamond", display.getIcon());
        assertTrue(display.getShowToast());
        assertEquals("Test Achievement", display.getTitle());
        assertEquals(1.5, display.getX());
        assertEquals(2.5, display.getY());
    }

    @Test
    void testPlayerAchievementEventWithFakeData() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);

        PlayerModel player = createFakePlayerModel();
        DisplayModel display = createFakeDisplayModel();
        AchievementModel achievement = createFakeAchievementModel(display);

        PlayerAchievementEvent event = new PlayerAchievementEvent(player, achievement);

        // 验证Achievement部分
        assertSame(achievement, event.getAchievement(), "AchievementModel should match");
        // 验证PlayerModel部分
        assertSame(player, event.getPlayer(), "PlayerModel should match");
        assertPlayerModel(event.getPlayer());
        // 验证AchievementModel的DisplayModel部分
        assertDisplayModel(event.getAchievement().getDisplay());
        // 补充事件本身的断言
        assertEquals("PlayerAchievementEvent", event.getEventName(), "EventName should be 'PlayerAchievementEvent'");
        assertEquals("notice", event.getPostType(), "PostType should be 'notice'");
        assertEquals("player_achievement", event.getSubType(), "SubType should be 'player_achievement'");
        assertTrue(event.getTimestamp() > 0, "Timestamp should be positive");
        assertNotNull(event.getServerName(), "ServerName should not be null");
    }
}
