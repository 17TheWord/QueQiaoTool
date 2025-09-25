package com.github.theword.queqiao.tool.event.base;

/** 通用基础玩家进度事件 */
public class BasePlayerAdvancementEvent extends BaseNoticeEvent {
  private final BaseAdvancement advancement;

  public BasePlayerAdvancementEvent(
      String eventName, BasePlayer player, BaseAdvancement advancement) {
    super(eventName, "achievement", player);
    this.advancement = advancement;
  }

  public static class BaseAdvancement {
    /** Common field */
    private String text;

    public BaseAdvancement() {}

    public BaseAdvancement(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }
  }
}
