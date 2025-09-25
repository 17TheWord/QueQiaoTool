package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * BaseEvent
 *
 * <p>基础事件
 *
 * <p>所有事件均继承该类
 */
public class BaseEvent {

    /** 事件名称 */
    @SerializedName("event_name")
    private final String eventName;

    /** 事件类型，如 message、notice、request 等 */
    @SerializedName("post_type")
    private final String postType;

    /** 事件子类型，如 chat、join、quit 等 */
    @SerializedName("sub_type")
    private final String subType;

    /** 时间戳，秒级 */
    private final int timestamp = (int) (System.currentTimeMillis() / 1000);

    /** 服务器名，每次生成事件通过配置文件获取 */
    @SerializedName("server_name")
    private String serverName = GlobalContext.getConfig().getServerName();

    /** 服务器版本号，工具初始化阶段传入 */
    @SerializedName("server_version")
    private String serverVersion = GlobalContext.getServerVersion();

    /** 服务器类型，工具初始化阶段传入 */
    @SerializedName("server_type")
    private String serverType = GlobalContext.getServerType();

    /**
     * 构造函数
     *
     * @param eventName 事件名称
     * @param postType  事件类型
     * @param subType   事件子类型
     */
    public BaseEvent(String eventName, String postType, String subType) {
        this.eventName = eventName;
        this.postType = postType;
        this.subType = subType;
    }

    /**
     * 将事件对象序列化为 JSON 字符串。
     *
     * @return 事件的 JSON 表示
     * @deprecated 请使用 {@link GsonUtils#buildGson()} 生成的 Gson 实例进行序列化
     */
    @Deprecated
    public String getJson() {
        Gson gson = GsonUtils.buildGson();
        return gson.toJson(this);
    }
}
