package com.github.theword.queqiao.tool.event.model.death;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 死亡信息模型
 *
 * @deprecated 自 0.6.0 起弃用，请使用全新的翻译组件 {@link com.github.theword.queqiao.tool.event.model.TranslateModel}
 */
@Deprecated
public class DeathModel {

    /**
     * 死亡信息唯一标识
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String key;

    /**
     * 死亡信息参数
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private List<?> args;

    /**
     * 死亡信息文本
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper、Spigot
     */
    private String text;

    public DeathModel() {
    }

    public DeathModel(String key, List<?> args, String text) {
        this.key = key;
        this.args = args;
        this.text = text;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<?> getArgs() {
        return args;
    }

    public void setArgs(Object args) {
        if (args == null) {
            this.args = Collections.emptyList();
        } else if (args instanceof List) {
            this.args = (List<?>) args;
        } else if (args instanceof Object[]) {
            this.args = Arrays.asList((Object[]) args);
        } else {
            this.args = Collections.singletonList(args);
        }
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
