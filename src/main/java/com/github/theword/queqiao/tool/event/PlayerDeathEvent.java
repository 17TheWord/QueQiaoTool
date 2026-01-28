package com.github.theword.queqiao.tool.event;

import com.github.theword.queqiao.tool.event.model.PlayerModel;
import com.github.theword.queqiao.tool.event.model.TranslateModel;
import com.github.theword.queqiao.tool.event.player.PlayerNoticeEvent;

/**
 * 玩家死亡事件
 *
 * @since 0.4.0
 */
public final class PlayerDeathEvent extends PlayerNoticeEvent {

    /**
     * 死亡信息数据模型。
     * <p>
     * ️ <b>重大变更：</b> 自 0.6.0 起，为了支持多语言翻译，
     * 该字段类型由 {@code DeathModel} 变更为 {@link TranslateModel}。
     * </p>
     *
     * @see TranslateModel
     * @since 0.6.0
     */
    private final TranslateModel death;

    /**
     * 构造函数
     * <p>
     * <b>重大变更：</b> 自 0.6.0 起，参数 {@code deathModel} 的类型由 {@code DeathModel} 变更为 {@link TranslateModel}。
     * </p>
     *
     * @param playerModel 触发事件的玩家
     * @param deathModel  死亡信息的翻译模型
     */
    public PlayerDeathEvent(PlayerModel playerModel, TranslateModel deathModel) {
        super("PlayerDeathEvent", "player_death", playerModel);
        this.death = deathModel;
    }

    public TranslateModel getDeath() {
        return death;
    }

}
