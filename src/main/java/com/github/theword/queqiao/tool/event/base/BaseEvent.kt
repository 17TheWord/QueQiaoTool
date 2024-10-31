package com.github.theword.queqiao.tool.event.base;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Setter;

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
    private String serverName;

    public BaseEvent(String eventName, String postType, String subType) {
        this.eventName = eventName;
        this.postType = postType;
        this.subType = subType;
    }

    public String getJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
