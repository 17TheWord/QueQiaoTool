package com.github.theword.queqiao.tool.utils;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GsonUtilsTest {

    static class TestObj {
        public String name;
        public int age;

        public TestObj() {
        }

        public TestObj(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @Test
    void testGetGsonSingleton() {
        Gson gson1 = GsonUtils.getGson();
        Gson gson2 = GsonUtils.getGson();
        assertSame(gson1, gson2, "GsonUtils.getGson() 应返回单例");
    }

    @Test
    void testSerializeDeserialize() {
        TestObj obj = new TestObj("Alice", 20);
        Gson gson = GsonUtils.getGson();
        String json = gson.toJson(obj);
        assertTrue(json.contains("Alice"));
        TestObj obj2 = gson.fromJson(json, TestObj.class);
        assertEquals("Alice", obj2.name);
        assertEquals(20, obj2.age);
    }

    @Test
    void testSerializeMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", 123);
        map.put("k2", "v2");
        Gson gson = GsonUtils.getGson();
        String json = gson.toJson(map);
        assertTrue(json.contains("k1"));
        Map result = gson.fromJson(json, Map.class);
        assertEquals(123.0, result.get("k1")); // Gson默认数字为Double
        assertEquals("v2", result.get("k2"));
    }
}

