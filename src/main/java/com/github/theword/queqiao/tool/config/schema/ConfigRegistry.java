package com.github.theword.queqiao.tool.config.schema;

import com.github.theword.queqiao.tool.config.exception.ConfigValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 配置注册中心（Schema）
 *
 * <p>职责：<b>注册配置项与区块、按路径查找、遍历整棵树、检测重复与父子冲突</b>。
 *
 * <p><b>区块由配置项的路径自动派生</b>：注册 {@code websocket_server.enable} 会自动建立
 * {@code websocket_server} 区块；注册 {@code addons.llm.model} 会自动建立
 * {@code addons} 与 {@code addons.llm}。因此显式注册区块<b>只用于补充注释与空行排版</b>。
 *
 * <p><b>并发</b>：读用读锁、注册用写锁；写盘等长操作应先取 {@link #snapshot()}，
 * 再基于快照工作，不必长时间持锁。
 *
 * @since 0.6.12
 */
public final class ConfigRegistry {

    /**
     * 根区块路径
     */
    public static final String ROOT_PATH = "";

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 已注册配置项（LinkedHashMap 保证声明顺序稳定）
     */
    private final Map<String, ConfigKey<?>> keysByPath = new LinkedHashMap<>();

    /**
     * 区块的排版元数据（注释 / 前后空行）；子节点由配置项路径派生，不在此保存
     */
    private final Map<String, ConfigSectionNode> sectionMetadata = new LinkedHashMap<>();

    private boolean frozen;

    /**
     * 注册一个配置项
     *
     * @param key 配置项定义
     * @throws ConfigValidationException 路径重复，或与已有配置项构成父子冲突
     */
    public void register(ConfigKey<?> key) {
        if (key == null) {
            throw new IllegalArgumentException("配置项不能为 null");
        }
        lock.writeLock().lock();
        try {
            requireMutable();
            String path = key.getPath();

            if (keysByPath.containsKey(path)) {
                throw new ConfigValidationException(path, "重复的配置路径");
            }
            if (sectionMetadata.containsKey(path)) {
                throw new ConfigValidationException(path, "该路径已作为区块存在（其下有配置项），不能再注册为配置项");
            }
            for (String ancestor : ancestorsOf(path)) {
                if (keysByPath.containsKey(ancestor)) {
                    throw new ConfigValidationException(path, "父路径 " + ancestor + " 已是配置项，不能再注册其子项");
                }
            }
            for (String existingPath : keysByPath.keySet()) {
                if (existingPath.length() > path.length() + 1 && existingPath.startsWith(path + ".")) {
                    throw new ConfigValidationException(
                            path, "该路径下已有配置项 " + existingPath + "，本路径只能作为区块，不能再注册为配置项");
                }
            }

            keysByPath.put(path, key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 注册（或更新）一个区块的排版元数据
     *
     * <p>区块路径通常已由配置项自动派生；此处用于补充注释与前后空行。
     *
     * @param section 区块
     * @throws ConfigValidationException 该路径已是配置项
     */
    public void register(ConfigSectionNode section) {
        if (section == null) {
            throw new IllegalArgumentException("区块不能为 null");
        }
        lock.writeLock().lock();
        try {
            requireMutable();
            String path = section.getPath();
            if (keysByPath.containsKey(path)) {
                throw new ConfigValidationException(path, "该路径已是配置项，不能再注册为区块");
            }
            sectionMetadata.put(path, section);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 按路径查找配置项
     *
     * @param path 点分路径
     * @return 配置项；未注册时返回 null
     */
    public ConfigKey<?> findByPath(String path) {
        lock.readLock().lock();
        try {
            return keysByPath.get(path);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return 已注册配置项数量
     */
    public int size() {
        lock.readLock().lock();
        try {
            return keysByPath.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 冻结注册中心。运行时启动完成配置声明后调用；之后不得再注册配置项或区块。
     */
    public void freeze() {
        lock.writeLock().lock();
        try {
            frozen = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @return 注册中心是否已冻结
     */
    public boolean isFrozen() {
        lock.readLock().lock();
        try {
            return frozen;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 生成不可变 Schema 快照
     *
     * <p>子节点顺序 = <b>声明顺序</b>：区块在"它的第一个配置项出现的位置"被挂到父区块上，
     * 因此同一份 Schema 多次生成的 YAML 结构完全一致（方案 §32.2 要求 deterministic）。
     *
     * @return 配置树快照
     */
    public ConfigTree snapshot() {
        lock.readLock().lock();
        try {
            List<ConfigKey<?>> keys = new ArrayList<>(keysByPath.values());

            // 1. 建立所有区块的构造器（根 + 每个配置项的祖先链 + 显式注册的区块）
            Map<String, SectionBuilder> builders = new LinkedHashMap<>();
            builders.put(ROOT_PATH, new SectionBuilder(ROOT_PATH));
            for (ConfigKey<?> key : keys) {
                for (String ancestor : ancestorsOf(key.getPath())) {
                    if (!builders.containsKey(ancestor)) {
                        builders.put(ancestor, new SectionBuilder(ancestor));
                    }
                }
            }
            for (String sectionPath : sectionMetadata.keySet()) {
                if (!builders.containsKey(sectionPath)) {
                    builders.put(sectionPath, new SectionBuilder(sectionPath));
                }
            }

            // 2. 按声明顺序填充：区块在首个配置项出现处挂到父区块上
            for (ConfigKey<?> key : keys) {
                List<String> ancestors = ancestorsOf(key.getPath());
                for (int i = 0; i < ancestors.size(); i++) {
                    String ancestor = ancestors.get(i);
                    String ancestorParent = i == 0 ? ROOT_PATH : ancestors.get(i - 1);
                    if (!builders.get(ancestor).attached) {
                        builders.get(ancestor).attached = true;
                        builders.get(ancestorParent).children.add(builders.get(ancestor));
                    }
                }
                String parentPath = ancestors.isEmpty() ? ROOT_PATH : ancestors.get(ancestors.size() - 1);
                builders.get(parentPath).children.add(key);
            }

            return new ConfigTree(build(ROOT_PATH, builders), keys, keysByPath);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 递归把构造器树转成不可变节点，并套用区块的注释/空行元数据
     */
    private ConfigSectionNode build(String path, Map<String, SectionBuilder> builders) {
        SectionBuilder builder = builders.get(path);
        ConfigSectionNode section = sectionMetadata.containsKey(path)
                ? sectionMetadata.get(path)
                : ConfigSectionNode.of(path);

        for (Object child : builder.children) {
            if (child instanceof SectionBuilder) {
                section = section.withChild(build(((SectionBuilder) child).path, builders));
            } else {
                section = section.withChild((ConfigNode) child);
            }
        }
        return section;
    }

    /**
     * 快照构建期的可变区块
     */
    private static final class SectionBuilder {

        private final String path;
        private final List<Object> children = new ArrayList<>();
        private boolean attached;

        private SectionBuilder(String path) {
            this.path = path;
        }
    }

    /**
     * 路径的全部祖先路径（自顶向下，不含自身）
     *
     * @param path 点分路径
     * @return 祖先路径列表
     */
    private static List<String> ancestorsOf(String path) {
        List<String> ancestors = new ArrayList<>();
        int index = path.indexOf('.');
        while (index >= 0) {
            ancestors.add(path.substring(0, index));
            index = path.indexOf('.', index + 1);
        }
        return ancestors.isEmpty() ? Collections.<String>emptyList() : ancestors;
    }

    private static String parentPathOf(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? ROOT_PATH : path.substring(0, index);
    }

    private void requireMutable() {
        if (frozen) {
            throw new IllegalStateException("ConfigRegistry 已冻结，配置项只能在启动期注册");
        }
    }
}
