package io.github.theword.queqiao.core.config.codec;

import io.github.theword.queqiao.core.config.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 编解码器测试
 *
 * <p>首期实现 4 种 codec（Boolean / Integer / String / List&lt;String&gt;）即可覆盖全部 23 个配置项。
 */
class ConfigCodecTest {

    @Test
    @DisplayName("BooleanCodec：只接受 Boolean")
    void booleanCodec() {
        assertEquals(Boolean.TRUE, BooleanCodec.INSTANCE.read("a.b", Boolean.TRUE));
        assertEquals("boolean", BooleanCodec.INSTANCE.typeName());
        assertEquals(Boolean.TRUE, BooleanCodec.INSTANCE.write(Boolean.TRUE));

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> BooleanCodec.INSTANCE.read("a.b", "yes"));
        assertTrue(e.getMessage().contains("a.b"), e.getMessage());
        assertTrue(e.getMessage().contains("boolean"), e.getMessage());
    }

    @Test
    @DisplayName("IntegerCodec：严格整数，8080.0 不接受")
    void integerCodecIsStrict() {
        assertEquals(8080, IntegerCodec.INSTANCE.read("a.port", 8080));
        assertEquals("integer", IntegerCodec.INSTANCE.typeName());

        assertThrows(ConfigValidationException.class, () -> IntegerCodec.INSTANCE.read("a.port", 8080.0));
        assertThrows(ConfigValidationException.class, () -> IntegerCodec.INSTANCE.read("a.port", "8080"));
    }

    @Test
    @DisplayName("StringCodec：null 视为无效配置（Phase 4 §13），非字符串类型报错")
    void stringCodec() {
        assertEquals("hello", StringCodec.INSTANCE.read("a.s", "hello"));
        assertEquals("", StringCodec.INSTANCE.read("a.s", ""), "显式空串仍然合法");
        assertEquals("string", StringCodec.INSTANCE.typeName());

        assertThrows(ConfigValidationException.class, () -> StringCodec.INSTANCE.read("a.s", null));
        assertThrows(ConfigValidationException.class, () -> StringCodec.INSTANCE.read("a.s", 123));
    }

    @Test
    @DisplayName("ListCodec：解析字符串列表；null 视为无效配置（Phase 4 §13）")
    void listCodec() {
        assertEquals(Arrays.asList("a", "b"), ListCodec.INSTANCE.read("a.list", Arrays.asList("a", "b")));
        assertEquals(0, ListCodec.INSTANCE.read("a.list", new ArrayList<String>()).size(), "显式空列表合法");
        assertEquals("list of string", ListCodec.INSTANCE.typeName());

        assertThrows(ConfigValidationException.class, () -> ListCodec.INSTANCE.read("a.list", null));
        ConfigValidationException nullElement = assertThrows(
                ConfigValidationException.class,
                () -> ListCodec.INSTANCE.read("a.list", Arrays.asList("a", null)));
        assertTrue(nullElement.getMessage().contains("1"), nullElement.getMessage());
        assertThrows(ConfigValidationException.class, () -> ListCodec.INSTANCE.read("a.list", "not-a-list"));
        assertThrows(ConfigValidationException.class, () -> ListCodec.INSTANCE.read("a.list", Arrays.asList(1, 2)));
    }

    @Test
    @DisplayName("ListCodec.copy：返回新实例（默认值防共享）")
    void listCodecCopyReturnsNewInstance() {
        List<String> source = Arrays.asList("a", "b");

        List<String> copy = ListCodec.INSTANCE.copy(source);

        assertEquals(source, copy);
        assertNotSame(source, copy);
        copy.add("c");
        assertEquals(2, source.size());
    }
}
