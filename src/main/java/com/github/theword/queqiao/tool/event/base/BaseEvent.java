package com.github.theword.queqiao.tool.event.base;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Setter;

import static com.github.theword.queqiao.tool.utils.Tool.config;
import static com.github.theword.queqiao.tool.utils.Tool.SERVER_VERSION;
import static com.github.theword.queqiao.tool.utils.Tool.SERVER_TYPE;

/**
 * BaseEvent
 * <p> 基础事件 </p>
 * <p> 所有事件均继承该类 </p>
 */
public class BaseEvent {

    /**
     * 事件名称
     */
    @SerializedName("event_name")
    private final String eventName;

    /**
     * 事件类型，如 message、notice、request 等
     */
    @SerializedName("post_type")
    private final String postType;

    /**
     * 事件子类型，如 chat、join、quit 等
     */
    @SerializedName("sub_type")
    private final String subType;

    /**
     * 时间戳，秒级
     */
    private final int timestamp = (int) (System.currentTimeMillis() / 1000);

    /**
     * 服务器名，每次生成事件通过配置文件获取
     */
    @Setter
    @SerializedName("server_name")
    private String serverName = config.getServerName();

    /**
     * 服务器版本号，工具初始化阶段传入
     */
    @Setter
    @SerializedName("server_version")
    private String serverVersion = SERVER_VERSION;

    /**
     * 服务器类型，工具初始化阶段传入
     */
    @Setter
    @SerializedName("server_type")
    private String serverType = SERVER_TYPE;

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

    public String getJson() {
        Gson gson = GsonUtils.buildGson();
        return gson.toJson(this);
    }
}
