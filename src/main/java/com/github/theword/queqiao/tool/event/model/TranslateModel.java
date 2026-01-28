package com.github.theword.queqiao.tool.event.model;

/**
 * 翻译模型类。
 * <p>
 * 该模型是 0.6.0 版本国际化体系的核心，用于替代原有的硬编码 String 字段。
 * 它支持 Minecraft 标准的翻译组件结构（Translation Component），包含翻译键、参数列表以及回退文本。
 * 由于参数本身也可以是翻译模型，该类支持递归嵌套，以处理复杂的复合消息。
 *
 * @since 0.6.0
 */
public class TranslateModel {

    /**
     * 翻译内容的唯一标识符（Translation Key）。
     * <p>
     * 例如：chat.type.advancement.task 或 death.attack.player。
     * 服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private String key;

    /**
     * 翻译键对应的参数列表。
     * <p>
     * 该字段支持递归嵌套。例如死亡消息中，杀手名或武器名可能本身也带有一个翻译键。
     * 服务端支持：Fabric、Forge、Folia、NeoForge、Paper
     */
    private TranslateModel[] args;

    /**
     * 翻译的回退文本。
     * <p>
     * 当本地翻译库中找不到对应的 key，或者翻译功能关闭时，将直接显示此原文（通常为英文或原始文本）。
     * 服务端支持：Fabric、Forge、Folia、NeoForge、Paper、Spigot
     */
    private String text;

    /**
     * 无参构造方法。
     * 供序列化框架（如 Gson）及反射逻辑使用。
     */
    public TranslateModel() {
    }

    /**
     * 全参数构造方法。
     *
     * @param key  翻译键
     * @param args 翻译参数数组
     * @param text 回退原文
     */
    public TranslateModel(String key, TranslateModel[] args, String text) {
        this.key = key;
        this.args = args;
        this.text = text;
    }

    /**
     * 获取当前组件的翻译键。
     *
     * @return 翻译键字符串
     */
    public String getKey() {
        return key;
    }

    /**
     * 设置当前组件的翻译键。
     *
     * @param key 翻译键字符串
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 获取翻译参数数组。
     *
     * @return 嵌套的翻译模型数组，可能为 null
     */
    public TranslateModel[] getArgs() {
        return args;
    }

    /**
     * 设置翻译参数数组。
     *
     * @param args 翻译模型数组
     */
    public void setArgs(TranslateModel[] args) {
        this.args = args;
    }

    /**
     * 获取回退文本或原始消息。
     *
     * @return 原始文本内容
     */
    public String getText() {
        return text;
    }

    /**
     * 设置回退文本。
     *
     * @param text 原始文本内容
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * 判断当前模型是否包含参数。
     *
     * @return 如果参数数组不为空且长度大于 0，返回 true；否则返回 false。
     */
    public boolean hasArgs() {
        return args != null && args.length > 0;
    }
}