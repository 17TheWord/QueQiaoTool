package com.github.theword.queqiao.tool.config.io;

import com.github.theword.queqiao.tool.config.ConfigKey;
import com.github.theword.queqiao.tool.config.ConfigRegistry;
import com.github.theword.queqiao.tool.config.Config;
import com.github.theword.queqiao.tool.config.codec.StringCodec;
import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigDocumentTest {

    @Test
    @DisplayName("构造后与原始嵌套集合隔离，暴露的集合不可修改")
    @SuppressWarnings("unchecked")
    void documentIsDeeplyImmutable() {
        List<Object> list = new ArrayList<>();
        list.add("original");
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("list", list);
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("unknown", nested);

        ConfigDocument document = new ConfigDocument(raw);
        list.add("changed");
        nested.put("new_key", "changed");

        Map<String, Object> storedNested = (Map<String, Object>) document.get("unknown");
        List<Object> storedList = (List<Object>) storedNested.get("list");
        assertEquals(java.util.Collections.singletonList("original"), storedList);
        assertThrows(UnsupportedOperationException.class, () -> document.getRoot().put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> storedNested.put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> storedList.add("x"));
    }

    @Test
    @DisplayName("写盘快照与原始文档隔离")
    void writeSnapshotKeepsOriginalDocumentState() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("unknown", "original");
        ConfigDocument document = new ConfigDocument(raw);
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKey<String> key = ConfigKey.builder("known", StringCodec.INSTANCE).defaultValue("value").build();
        registry.register(key);
        Config runtime = new Config(registry);

        ConfigWriteSnapshot snapshot = ConfigWriteSnapshot.of(registry, runtime, document);
        raw.put("unknown", "changed");

        assertEquals("known: value\nunknown: original\n", new ConfigWriter().render(snapshot));
    }

    @Test
    @DisplayName("嵌套 Mapping 的非字符串键被拒绝")
    void rejectsNonStringNestedMapKeys() {
        Map<Object, Object> nested = new LinkedHashMap<>();
        nested.put(1, "value");
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("section", nested);

        assertThrows(ConfigValidationException.class, () -> new ConfigDocument(raw));
    }
}
