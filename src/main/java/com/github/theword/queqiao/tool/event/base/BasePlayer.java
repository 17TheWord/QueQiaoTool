package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import java.util.UUID;

/**
 * 玩家基础信息
 *
 * <p>包含玩家的昵称和 UUID
 *
 * <p>用于标识一个玩家
 */
public class BasePlayer {

  /** 玩家昵称 */
  private String nickname;

  /** 玩家 UUID */
  private UUID uuid;

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public BasePlayer() {}

  public BasePlayer(String nickname, UUID uuid) {
    this.nickname = nickname;
    this.uuid = uuid;
  }

  /**
   * 判断两个玩家是否为同一个玩家
   *
   * <p>玩家对象中，nickname 和 uuid 至少有一个不为空
   *
   * <p>首先 判断 uuid 是否相等，若相等则返回 true
   *
   * <p>若不相等，则判断 nickname 是否相等，若相等则返回 true
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
   * @deprecated 请使用 {@link GsonUtils#buildGson()} 生成的 Gson 对象进行转换
   */
  @Deprecated
  public String getJson() {
    return GsonUtils.buildGson().toJson(this);
  }
}
