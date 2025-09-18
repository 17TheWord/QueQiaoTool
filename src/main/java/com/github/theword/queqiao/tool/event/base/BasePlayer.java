package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 玩家基础信息
 * <p> 包含玩家的昵称和 UUID </p>
 * <p> 用于标识一个玩家 </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePlayer {

    /**
     * 玩家昵称
     */
    private String nickname;

    /**
     * 玩家 UUID
     */
    private UUID uuid;

    /**
     * 判断两个玩家是否为同一个玩家
     * <p> 玩家对象中，nickname 和 uuid 至少有一个不为空 </p>
     * <p> 首先 判断 uuid 是否相等，若相等则返回 true </p>
     * <p> 若不相等，则判断 nickname 是否相等，若相等则返回 true </p>
     *
     * @param o 对比对象
     * @return 是否为同一个玩家
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BasePlayer)) return false;
        if (this == o) return true;
        if (uuid != null && uuid.equals(((BasePlayer) o).uuid)) return true;
        return nickname != null && nickname.equals(((BasePlayer) o).nickname);
    }

    @Override
    public int hashCode() {
        return nickname.hashCode();
    }

    /**
     * 将玩家对象转换为 JSON 字符串
     *
     * @return JSON 字符串
     */
    public String getJson() {
        return GsonUtils.buildGson().toJson(this);
    }
}
