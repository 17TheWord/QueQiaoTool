package io.github.theword.queqiao.core.event;

import io.github.theword.queqiao.core.event.model.PlayerModel;
import io.github.theword.queqiao.core.event.model.TranslateModel;
import io.github.theword.queqiao.core.event.model.achievement.AchievementModel;
import io.github.theword.queqiao.core.event.model.achievement.DisplayModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAchievementEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    void setUp() {
    }

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
        display.setFrame("goal");

        TranslateModel title = new TranslateModel("advancements.story.root.title", null, "Stone Age");
        TranslateModel description = new TranslateModel("advancements.story.root.description", null, "Mine stone with your new pickaxe");

        display.setTitle(title);
        display.setDescription(description);
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
        assertTrue(player.getOp());
    }

    private void assertDisplayModel(DisplayModel display) {
        assertNotNull(display);
        assertEquals("goal", display.getFrame());

        // 验证标题翻译组件
        assertNotNull(display.getTitle());
        assertEquals("advancements.story.root.title", display.getTitle().getKey());
        assertEquals("Stone Age", display.getTitle().getText());

        // 验证描述翻译组件
        assertNotNull(display.getDescription());
        assertEquals("advancements.story.root.description", display.getDescription().getKey());
    }

    @Test
    void testPlayerAchievementEventWithRealConfig() {
        PlayerModel player = createFakePlayerModel();
        DisplayModel display = createFakeDisplayModel();
        AchievementModel achievement = createFakeAchievementModel(display);

        PlayerAchievementEvent event = new PlayerAchievementEvent(player, achievement);

        // 验证事件基础属性，serverName 会从 config.yml 的默认值中获取
        assertEquals("PlayerAchievementEvent", event.getEventName());
        assertEquals("notice", event.getPostType());
        assertEquals("player_achievement", event.getSubType());
        // WS-F：服务器上下文由发布时填充，构造不再依赖全局状态
        assertNull(event.getServerName(), "构造后尚未填充");
        event.fillServerContext("TestServer", "1.20.1", "spigot");
        assertEquals("TestServer", event.getServerName());
        assertEquals("1.20.1", event.getServerVersion());
        assertEquals("spigot", event.getServerType());
        assertTrue(event.getTimestamp() > 0);

        // 验证数据模型
        assertSame(player, event.getPlayer());
        assertSame(achievement, event.getAchievement());

        assertPlayerModel(event.getPlayer());
        assertDisplayModel(event.getAchievement().getDisplay());
    }
}