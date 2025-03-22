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
 * <p>
 * serverName 将在发送前通过配置文件获取并填充
 */
public class BaseEvent {
    @SerializedName("event_name")
    private final String eventName;
    @SerializedName("post_type")
    private final String postType;
    @SerializedName("sub_type")
    private final String subType;
    private final int timestamp = (int) (System.currentTimeMillis() / 1000);
    @Setter
    @SerializedName("server_name")
    private String serverName = config.getServerName();
    @Setter
    @SerializedName("server_version")
    private String serverVersion = SERVER_VERSION;
    @Setter
    @SerializedName("server_type")
    private String serverType = SERVER_TYPE;

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
