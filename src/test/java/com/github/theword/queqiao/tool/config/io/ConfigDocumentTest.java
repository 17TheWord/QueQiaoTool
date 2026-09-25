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
import java.util.Date;

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

    @Test
    @DisplayName("Date 与二进制值也不会通过构造参数或读取结果泄漏引用")
    @SuppressWarnings("unchecked")
    void mutableYamlScalarsAreCopiedOnRead() {
        Date originalDate = new Date(1000L);
        byte[] originalBytes = new byte[] {1, 2};
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("date", originalDate);
        raw.put("binary", originalBytes);
        ConfigDocument document = new ConfigDocument(raw);

        originalDate.setTime(2000L);
        originalBytes[0] = 9;
        Date fromGet = (Date) document.get("date");
        byte[] bytesFromGet = (byte[]) document.get("binary");
        fromGet.setTime(3000L);
        bytesFromGet[0] = 8;

        assertEquals(1000L, ((Date) document.get("date")).getTime());
        assertEquals(1, ((byte[]) document.get("binary"))[0]);
        Map<String, Object> root = document.getRoot();
        ((Date) root.get("date")).setTime(4000L);
        ((byte[]) root.get("binary"))[0] = 7;
        assertEquals(1000L, ((Date) document.get("date")).getTime());
        assertEquals(1, ((byte[]) document.get("binary"))[0]);
    }
}
