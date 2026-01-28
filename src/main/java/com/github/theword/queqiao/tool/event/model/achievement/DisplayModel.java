package com.github.theword.queqiao.tool.event.model.achievement;

import com.github.theword.queqiao.tool.event.model.TranslateModel;

/**
 * 成就显示模型
 */
public class DisplayModel {

    /**
     * 展示信息的标题组件。
     * <p>
     * 架构变更说明：自 0.6.0 起，该字段类型由 String 升级为 TranslateModel。
     * 这一变更允许标题包含嵌套的翻译键或富文本组件，提供了比纯文本字符串更强的表达能力。
     * <p>
     * 服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     *
     * @since 0.6.0
     */
    private TranslateModel title;

    /**
     * 展示信息的详细描述组件。
     * <p>
     * 架构变更说明：自 0.6.0 起，该字段类型由 String 升级为 TranslateModel。
     * 该组件支持多语言本地化描述，能够根据客户端的语言环境配置动态渲染对应内容。
     * <p>
     * 服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     *
     * @since 0.6.0
     */
    private TranslateModel description;

    /**
     * 展示信息类型
     *
     * <p>服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String frame;

    public TranslateModel getDescription() {
        return description;
    }

    public void setDescription(TranslateModel value) {
        this.description = value;
    }

    public String getFrame() {
        return frame;
    }

    public void setFrame(String value) {
        this.frame = value.toLowerCase();
    }

    public TranslateModel getTitle() {
        return title;
    }

    public void setTitle(TranslateModel value) {
        this.title = value;
    }

}