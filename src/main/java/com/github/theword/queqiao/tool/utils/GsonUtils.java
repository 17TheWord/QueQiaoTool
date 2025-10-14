package com.github.theword.queqiao.tool.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;

/**
 * Gson Utils
 * <p>
 * 提供全局唯一的 Gson 实例，避免重复初始化。
 * 可在此统一配置 Gson（如字段排除、命名策略等）。
 */
public class GsonUtils {
    /**
     * 全局 Gson 实例
     */
    private static final Gson INSTANCE = new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT).create();

    /**
     * 获取全局 Gson 实例
     *
     * @return Gson 单例
     */
    public static Gson getGson() {
        return INSTANCE;
    }
}
