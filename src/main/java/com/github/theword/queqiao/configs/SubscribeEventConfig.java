package com.github.theword.queqiao.configs;

import lombok.Data;

@Data
public class SubscribeEventConfig {
    private boolean player_chat = true;
    private boolean player_death = true;
    private boolean player_login = true;
    private boolean player_quit = true;
    private boolean player_command = false;
}
