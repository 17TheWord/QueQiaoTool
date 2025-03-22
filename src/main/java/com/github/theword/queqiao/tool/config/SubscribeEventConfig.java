package com.github.theword.queqiao.tool.config;

import lombok.Data;

@Data
public class SubscribeEventConfig {
    private boolean playerChat = true;
    private boolean playerDeath = true;
    private boolean playerJoin = true;
    private boolean playerQuit = true;
    private boolean playerCommand = true;
    private boolean playerAdvancement = true;
}
