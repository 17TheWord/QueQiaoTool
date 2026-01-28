package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.TranslateModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDeathEventTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @BeforeEach
    void setUp() {
        Config config = Config.loadConfig(false, logger);
        GlobalContext.setConfig(config);
    }

    private PlayerModel createFakePlayerModel() {
        PlayerModel player = new PlayerModel();
        player.setNickname("TestPlayer");
        player.setUuid(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        player.setAddress("127.0.0.1");
        player.setHealth(0.0); // 死亡事件中生命值通常为 0
        player.setMaxHealth(20.0);
        player.setExperienceLevel(5);
        player.setExperienceProgress(0.5);
        player.setTotalExperience(100);
        player.setOp(true);
        player.setWalkSpeed(0.2);
        player.setX(100.0);
        player.setY(64.0);
        player.setZ(100.0);
        return player;
    }

    private TranslateModel createFakeDeathTranslateModel() {
        TranslateModel weapon = new TranslateModel("item.minecraft.diamond_sword", null, "Diamond Sword");
        TranslateModel killer = new TranslateModel(null, null, "Zombie");

        // 组装 Args
        TranslateModel[] args = new TranslateModel[]{
                new TranslateModel(null, null, "TestPlayer"),
                killer,
                weapon
        };

        return new TranslateModel("death.attack.player.item", args, "TestPlayer was slain by Zombie using Diamond Sword");
    }

    private void assertPlayerModel(PlayerModel player) {
        assertEquals("TestPlayer", player.getNickname());
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), player.getUuid());
        assertEquals(0.0, player.getHealth());
        assertTrue(player.getOp());
    }

    @Test
    void testPlayerDeathEventWithTranslateModel() {
        PlayerModel player = createFakePlayerModel();
        TranslateModel death = createFakeDeathTranslateModel();

        PlayerDeathEvent event = new PlayerDeathEvent(player, death);

        // 验证事件基础属性，serverName 由真实 Config 提供
        assertEquals("PlayerDeathEvent", event.getEventName());
        assertEquals("notice", event.getPostType());
        assertEquals("player_death", event.getSubType());
        assertNotNull(event.getServerName());
        assertTrue(event.getTimestamp() > 0);

        // 验证 PlayerModel 关联
        assertSame(player, event.getPlayer());
        assertPlayerModel(event.getPlayer());

        // 验证 TranslateModel 关联及其递归结构
        assertSame(death, event.getDeath());
        assertEquals("death.attack.player.item", event.getDeath().getKey());
        assertTrue(event.getDeath().hasArgs());
        assertEquals(3, event.getDeath().getArgs().length);

        // 验证参数中的递归逻辑
        assertEquals("item.minecraft.diamond_sword", event.getDeath().getArgs()[2].getKey());
    }
}