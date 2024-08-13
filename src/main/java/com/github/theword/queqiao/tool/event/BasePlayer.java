package com.github.theword.queqiao.tool.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePlayer {
    private String nickname;
    private UUID uuid;

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
}
