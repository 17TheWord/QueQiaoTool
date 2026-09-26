package io.github.theword.queqiao.core.config;

import io.github.theword.queqiao.core.config.codec.BooleanCodec;
import io.github.theword.queqiao.core.config.codec.IntegerCodec;
import io.github.theword.queqiao.core.config.exception.ConfigValidationException;
import io.github.theword.queqiao.core.config.schema.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigRegistry} 测试
 *
 * <p>覆盖方案 §53 要求的：register、lookup、duplicate、父子冲突、遍历、snapshot。
 */
class ConfigRegistryTest {

    private static ConfigKey<Boolean> boolKey(String path) {
        return ConfigKey.builder(path, BooleanCodec.INSTANCE).defaultValue(true).build();
    }

    private static ConfigKey<Integer> intKey(String path) {
        return ConfigKey.builder(path, IntegerCodec.INSTANCE).defaultValue(1).build();
    }

    @Test
    @DisplayName("注册与查找")
    void registerAndLookup() {
        ConfigRegistry registry = new ConfigRegistry();
        ConfigKey<Boolean> key = boolKey("websocket_server.enable");
        registry.register(key);

        assertEquals(key, registry.findByPath("websocket_server.enable"));
        assertNull(registry.findByPath("websocket_server.host"));
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("重复路径直接抛错，不静默覆盖")
    void duplicatePathIsRejected() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(boolKey("a.b"));

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> registry.register(boolKey("a.b")));
        assertTrue(e.getMessage().contains("a.b"), e.getMessage());
        assertTrue(e.getMessage().contains("重复"), e.getMessage());
    }

    @Test
    @DisplayName("父子冲突：父已是配置项时不能注册子项")
    void leafParentConflictIsRejected() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("a.b"));

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> registry.register(intKey("a.b.c")));
        assertTrue(e.getMessage().contains("a.b"), e.getMessage());
    }

    @Test
    @DisplayName("父子冲突：已存在子项时不能再把父注册为配置项")
    void parentLeafConflictIsRejected() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("a.b.c"));

        ConfigValidationException e =
                assertThrows(ConfigValidationException.class, () -> registry.register(intKey("a.b")));
        assertTrue(e.getMessage().contains("a.b"), e.getMessage());
    }

    @Test
    @DisplayName("区块路径不能与配置项路径重合")
    void sectionAndKeyCannotSharePath() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("a.b"));

        assertThrows(ConfigValidationException.class, () -> registry.register(ConfigSectionNode.of("a.b")));
    }

    @Test
    @DisplayName("区块由配置项路径自动派生")
    void sectionsAreDerivedFromKeys() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("addons.llm.model"));

        ConfigTree tree = registry.snapshot();
        ConfigSectionNode root = tree.getRoot();

        ConfigSectionNode addons = childSection(root, "addons");
        assertNotNull(addons, "应自动派生 addons 区块");
        ConfigSectionNode llm = childSection(addons, "llm");
        assertNotNull(llm, "应自动派生 addons.llm 区块");
        assertEquals("addons.llm.model", llm.getChildren().get(0).getPath());
    }

    @Test
    @DisplayName("snapshot 保持注册顺序（生成 YAML 需要顺序稳定）")
    void snapshotKeepsDeclarationOrder() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("z.last"));
        registry.register(intKey("a.first"));
        registry.register(intKey("m.middle"));

        ConfigTree tree = registry.snapshot();

        assertEquals(3, tree.size());
        assertEquals("z.last", tree.getKeys().get(0).getPath());
        assertEquals("a.first", tree.getKeys().get(1).getPath());
        assertEquals("m.middle", tree.getKeys().get(2).getPath());
    }

    @Test
    @DisplayName("snapshot 是只读快照：注册新配置项不影响已取得的快照")
    void snapshotIsStable() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("a.b"));

        ConfigTree before = registry.snapshot();
        registry.register(intKey("c.d"));

        assertEquals(1, before.size(), "旧快照不应看到新注册的配置项");
        assertEquals(2, registry.snapshot().size());
        assertThrows(UnsupportedOperationException.class, () -> before.getKeys().add(null));
    }

    @Test
    @DisplayName("区块注释与空行元数据会进入快照")
    void sectionMetadataIsApplied() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("websocket_server.port"));
        registry.register(ConfigSectionNode.of("websocket_server")
                .comment("WebSocket Server 配置")
                .blankLinesBefore(2));

        ConfigSectionNode section = childSection(registry.snapshot().getRoot(), "websocket_server");
        assertNotNull(section);
        assertEquals(1, section.getCommentLines().size());
        assertEquals("WebSocket Server 配置", section.getCommentLines().get(0));
        assertEquals(2, section.getBlankLinesBefore());
    }

    @Test
    @DisplayName("冻结前可注册，冻结后拒绝核心与 Addon 配置")
    void freezeRejectsFurtherRegistration() {
        ConfigRegistry registry = new ConfigRegistry();
        registry.register(intKey("addons.llm.model"));
        registry.freeze();

        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class, () -> registry.register(intKey("core.value")));
        assertThrows(IllegalStateException.class, () -> registry.register(intKey("addons.ai.radius")));
        assertThrows(IllegalStateException.class, () -> registry.register(ConfigSectionNode.of("addons.ai")));
        assertEquals(1, registry.snapshot().size());
    }

    private static ConfigSectionNode childSection(ConfigSectionNode parent, String name) {
        for (ConfigNode child : parent.getChildren()) {
            if (child instanceof ConfigSectionNode && name.equals(child.getName())) {
                return (ConfigSectionNode) child;
            }
        }
        return null;
    }
}
