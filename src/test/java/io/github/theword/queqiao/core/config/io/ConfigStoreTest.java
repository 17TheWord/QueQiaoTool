package io.github.theword.queqiao.core.config.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ConfigStore} 测试
 *
 * <p>本类不在正常启动路径上被调用（正常启动只读不写），
 * 但它是未来 {@code ConfigMigration} 与显式"同步配置"能力的公共依赖，
 * 且备份轮换逻辑容易写错——因此需要真实测试。
 */
class ConfigStoreTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigStoreTest.class);

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path backup(Path target, int index) {
        String base = target.getFileName().toString() + ".bak";
        return target.resolveSibling(index == 0 ? base : base + "." + index);
    }

    @Test
    @DisplayName("原子写入：内容完整写入，且不残留临时文件")
    void writeAtomicallyWritesContent(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("config.yml");

        ConfigStore.writeAtomically(target, "hello: 世界\n", LOGGER);

        assertEquals("hello: 世界\n", read(target));
        try (java.util.stream.Stream<Path> files = Files.list(tempDir)) {
            assertTrue(
                    files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                    "不应残留临时文件");
        }
    }

    @Test
    @DisplayName("原子写入：大内容完整写入")
    void writeAtomicallyWritesLargeContentCompletely(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("config.yml");
        StringBuilder contentBuilder = new StringBuilder("value: ");
        for (int index = 0; index < 2 * 1024 * 1024; index++) {
            contentBuilder.append('x');
        }
        String content = contentBuilder.append('\n').toString();

        ConfigStore.writeAtomically(target, content, LOGGER);

        assertEquals(content, read(target));
    }

    @Test
    @DisplayName("原子写入：目标目录不存在时自动创建")
    void writeAtomicallyCreatesParentDirectories(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("a").resolve("b").resolve("config.yml");

        ConfigStore.writeAtomically(target, "x: 1\n", LOGGER);

        assertTrue(Files.exists(target));
    }

    @Test
    @DisplayName("备份轮换：最多保留 5 份，且 .bak 始终是最新一份")
    void rotateBackupKeepsAtMostFiveAndNewestIsBak(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("config.yml");

        // 连续写入 7 次，每次写入前备份
        for (int round = 1; round <= 7; round++) {
            Files.write(target, ("round: " + round + "\n").getBytes(StandardCharsets.UTF_8));
            ConfigStore.rotateBackup(target, LOGGER);
        }

        // 备份总数不超过 5
        int backupCount = 0;
        for (int index = 0; index < ConfigStore.MAX_BACKUPS; index++) {
            if (Files.exists(backup(target, index))) {
                backupCount++;
            }
        }
        assertEquals(ConfigStore.MAX_BACKUPS, backupCount, "应保留 5 份备份");
        assertFalse(Files.exists(backup(target, ConfigStore.MAX_BACKUPS)), "不应存在第 6 份备份");

        // .bak 是最近一次写入前的内容（第 7 轮备份时文件内容为 round: 7）
        assertEquals("round: 7\n", read(backup(target, 0)), ".bak 应为最新一份备份");
    }

    @Test
    @DisplayName("目标文件不存在时不产生备份")
    void rotateBackupSkipsMissingTarget(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("config.yml");

        ConfigStore.rotateBackup(target, LOGGER);

        assertFalse(Files.exists(backup(target, 0)), "目标不存在时不应产生备份");
    }

    @Test
    @DisplayName("backupAndWriteAtomically：先备份旧内容，再写入新内容")
    void backupAndWriteKeepsPreviousContent(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("config.yml");
        Files.write(target, "old: 1\n".getBytes(StandardCharsets.UTF_8));

        ConfigStore.backupAndWriteAtomically(target, "new: 2\n", LOGGER);

        assertEquals("new: 2\n", read(target), "目标应为新内容");
        assertEquals("old: 1\n", read(backup(target, 0)), "备份应为写入前的内容");
    }
}
