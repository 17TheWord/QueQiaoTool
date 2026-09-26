package io.github.theword.queqiao.core.event.base;

import com.google.gson.annotations.SerializedName;

/**
 * BaseEvent
 *
 * <p>基础事件
 *
 * <p>所有事件的基类
 *
 * <p><b>不依赖全局状态</b>：服务器上下文（服务器名 / 版本 / 类型）不再在字段初始化器中
 * 从 {@code GlobalContext} 读取，而是由发布方通过
 * {@link #fillServerContext(String, String, String)} 在序列化前填充。
 * 这样事件的<b>构造</b>是纯粹的数据组装，可在无全局上下文的环境下独立构造与测试。
 *
 * @since 0.6.11
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
     * 服务器名
     *
     * <p>由 {@link #fillServerContext(String, String, String)} 在发布前填充。
     */
    @SerializedName("server_name")
    private String serverName;

    /**
     * 服务器版本号
     *
     * <p>由 {@link #fillServerContext(String, String, String)} 在发布前填充。
     */
    @SerializedName("server_version")
    private String serverVersion;

    /**
     * 服务器类型
     *
     * <p>由 {@link #fillServerContext(String, String, String)} 在发布前填充。
     */
    @SerializedName("server_type")
    private String serverType;

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
     * 填充服务器上下文
     *
     * <p>由发布方在序列化前调用。平台应通过 {@code GlobalContext.sendEvent(...)} 发布事件，
     * 该路径会自动完成填充；若绕过发布路径直接序列化事件，
     * 这三个字段将保持为 {@code null}。
     *
     * @param serverName    服务器名
     * @param serverVersion 服务器版本号
     * @param serverType    服务器类型
     */
    public void fillServerContext(String serverName, String serverVersion, String serverType) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.serverType = serverType;
    }

    public String getEventName() {
        return eventName;
    }

    public String getPostType() {
        return postType;
    }

    public String getSubType() {
        return subType;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public String getServerType() {
        return serverType;
    }
}
