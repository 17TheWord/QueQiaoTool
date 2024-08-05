package com.github.theword.queqiao.events.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BasePlayer {
    private String nickname;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasePlayer that = (BasePlayer) o;
        return nickname.equals(that.nickname);
    }

    @Override
    public int hashCode() {
        return nickname.hashCode();
    }
}
