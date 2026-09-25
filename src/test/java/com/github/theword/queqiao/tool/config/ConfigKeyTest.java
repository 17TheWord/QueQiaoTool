package com.github.theword.queqiao.tool.config;

import com.github.theword.queqiao.tool.config.codec.BooleanCodec;
import com.github.theword.queqiao.tool.config.codec.IntegerCodec;
import com.github.theword.queqiao.tool.config.codec.ListCodec;
import com.github.theword.queqiao.tool.config.codec.StringCodec;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import com.github.theword.queqiao.tool.config.validation.ConfigValidators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigKey} 测试
 *
 * <p>覆盖方案 §53 要求的：path、default、comment、codec、validator。
 */
class ConfigKeyTest {

    @Test
    @DisplayName("路径与名称：完整路径来自声明，名称取最后一段")
    void pathAndName() {
        ConfigKey<Boolean> key = ConfigKey.builder("websocket_server.enable", BooleanCodec.INSTANCE)
                .defaultValue(true)
                .build();

        assertEquals("websocket_server.enable", key.getPath());
        assertEquals("enable", key.getName());
    }

    @Test
    @DisplayName("路径格式非法时直接抛错")
    void invalidPathIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigKey.builder("", BooleanCodec.INSTANCE).defaultValue(true).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigKey.builder("a..b", BooleanCodec.INSTANCE).defaultValue(true).build());
        assertThrows(
                IllegalArgumentException.class,
                () -> ConfigKey.builder("a.", BooleanCodec.INSTANCE).defaultValue(true).build());
    }

    @Test
    @DisplayName("未指定默认值时构建失败（fail fast）")
    void missingDefaultValueIsRejected() {
        ConfigKey.Builder<Integer> builder = ConfigKey.builder("a.b", IntegerCodec.INSTANCE);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    @DisplayName("注释：可多行，只读")
    void commentLines() {
        ConfigKey<String> key = ConfigKey.builder("a.b", StringCodec.INSTANCE)
                .defaultValue("x")
                .comment("第一行", "第二行")
                .build();

        assertEquals(Arrays.asList("第一行", "第二行"), key.getCommentLines());
        assertThrows(UnsupportedOperationException.class, () -> key.getCommentLines().add("第三行"));
    }

    @Test
    @DisplayName("codec：解析与类型描述")
    void codecIsUsed() {
        ConfigKey<Integer> key =
                ConfigKey.builder("a.port", IntegerCodec.INSTANCE).defaultValue(8080).build();

        assertEquals(9090, key.parse(9090));
        assertEquals(8080, key.defaultValue());
        assertEquals("integer", key.getCodec().typeName());
        assertThrows(ConfigValidationException.class, () -> key.parse("not-a-number"));
    }

    @Test
    @DisplayName("validator：解析后立即校验，越界抛出并带路径")
    void validatorRunsAfterParse() {
        ConfigKey<Integer> key = ConfigKey.builder("websocket_server.port", IntegerCodec.INSTANCE)
                .defaultValue(8080)
                .validator(ConfigValidators.range(1, 65535))
                .build();

        assertTrue(key.hasValidator());
        assertEquals(9090, key.parse(9090));

        ConfigValidationException e = assertThrows(ConfigValidationException.class, () -> key.parse(99999));
        assertTrue(e.getMessage().contains("websocket_server.port"), e.getMessage());
        assertTrue(e.getMessage().contains("65535"), e.getMessage());
    }

    @Test
    @DisplayName("默认值：可变类型每次返回副本，不在多个 Config 之间共享")
    void mutableDefaultIsCopied() {
        ConfigKey<List<String>> key = ConfigKey.builder("a.list", ListCodec.INSTANCE)
                .defaultValueSupplier(() -> Arrays.asList("a", "b"))
                .build();

        List<String> first = key.defaultValue();
        List<String> second = key.defaultValue();

        assertEquals(first, second);
        assertNotSame(first, second, "两次取默认值必须是不同实例，否则多个 Config 会共享同一个可变对象");

        first.add("c");
        assertEquals(2, second.size(), "修改一份默认值不应影响另一份");
    }

    @Test
    @DisplayName("equals/hashCode 使用对象 identity：path 相同也是不同 key")
    void identitySemantics() {
        ConfigKey<Boolean> a = ConfigKey.builder("a.b", BooleanCodec.INSTANCE).defaultValue(true).build();
        ConfigKey<Boolean> b = ConfigKey.builder("a.b", BooleanCodec.INSTANCE).defaultValue(true).build();

        assertFalse(a.equals(b), "不同对象即使 path 相同也不相等");
        assertEquals(a, a);
    }
}
