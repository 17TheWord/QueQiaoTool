package com.github.theword.queqiao.tool.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Tool#format} 单元测试
 *
 * <p>该工具用于让项目自己的消息统一使用 {@code {}} 占位符（与 SLF4J 一致），
 * 避免把 {@code {}} 模板交给 {@code String.format} 导致的"占位符原样输出"问题。
 */
class ToolFormatTest {

    @Test
    @DisplayName("单个占位符被替换")
    void replacesSinglePlaceholder() {
        assertEquals("连接至 ws://x 的 Client", Tool.format("连接至 {} 的 Client", "ws://x"));
    }

    @Test
    @DisplayName("多个占位符按出现顺序依次替换")
    void replacesPlaceholdersInOrder() {
        assertEquals(
                "连接至：ws://x 的 Client 正在关闭，Code 1000，Reason：重载。",
                Tool.format("连接至：{} 的 Client 正在关闭，Code {}，Reason：{}。", "ws://x", 1000, "重载"));
    }

    @Test
    @DisplayName("参数不足时剩余占位符保持原样")
    void keepsRemainingPlaceholdersWhenArgsAreInsufficient() {
        assertEquals("a=1, b={}", Tool.format("a={}, b={}", 1));
    }

    @Test
    @DisplayName("参数多余时被忽略")
    void ignoresExtraArguments() {
        assertEquals("a=1", Tool.format("a={}", 1, 2, 3));
    }

    @Test
    @DisplayName("参数为 null 时输出字符串 null")
    void rendersNullArgumentAsNullText() {
        assertEquals("a=null", Tool.format("a={}", (Object) null));
    }

    @Test
    @DisplayName("模板不含占位符时原样返回同一实例（零分配）")
    void returnsSameInstanceWhenNoPlaceholder() {
        String template = "Websocket 正在重载";
        assertSame(template, Tool.format(template, "ignored"));
    }

    @Test
    @DisplayName("无参数时原样返回同一实例（即使模板含占位符）")
    void returnsSameInstanceWhenNoArguments() {
        String template = "a={}, b={}";
        assertSame(template, Tool.format(template));
    }

    @Test
    @DisplayName("模板为 null 时返回字符串 null，不抛异常")
    void handlesNullTemplate() {
        assertEquals("null", Tool.format(null, "x"));
    }

    /**
     * 这条用例记录了本工具存在的<b>原因</b>：{@code String.format} 不认识 {@code {}}，
     * 会把模板原样返回且静默忽略参数——关闭帧原因里出现字面 {@code {}} 正是这么来的。
     */
    @Test
    @DisplayName("回归说明：{} 模板交给 String.format 会原样返回（本工具存在的原因）")
    void documentsWhyToolFormatExists() {
        String template = "连接至：{} 的 WebSocket Client 正在关闭，Code {}，Reason：{}。";

        String viaStringFormat = String.format(template, "ws://x", 1000, "重载");
        assertEquals(template, viaStringFormat, "String.format 对 {} 模板会原样返回、参数被忽略");
        assertTrue(viaStringFormat.contains("{}"), "这正是曾经导致关闭原因里带字面 {} 的原因");

        String viaToolFormat = Tool.format(template, "ws://x", 1000, "重载");
        assertFalse(viaToolFormat.contains("{}"), "Tool.format 应完成替换");
        assertTrue(viaToolFormat.contains("ws://x"), "应替换进第一个参数");
    }
}
