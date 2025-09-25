package com.github.theword.queqiao.tool.payload.modle.hover;

import com.github.theword.queqiao.tool.payload.modle.component.CommonBaseComponent;
import java.util.List;
import java.util.UUID;

public class CommonHoverEntity {
  /** Spigot, Forge, Fabric */
  String type;

  /** Spigot */
  String id;

  /** Spigot, Forge, Fabric */
  List<CommonBaseComponent> name;

  /** Velocity */
  UUID uuid;

  /** Velocity */
  String key;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<CommonBaseComponent> getName() {
    return name;
  }

  public void setName(List<CommonBaseComponent> name) {
    this.name = name;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public CommonHoverEntity() {}

  public CommonHoverEntity(
      String type, String id, List<CommonBaseComponent> name, UUID uuid, String key) {
    this.type = type;
    this.id = id;
    this.name = name;
    this.uuid = uuid;
    this.key = key;
  }
}
