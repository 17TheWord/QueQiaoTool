package com.github.theword.queqiao.tool.payload;

import com.google.gson.JsonElement;

/**
 * BasePayload
 *
 * <p>api：API名称
 *
 * <p>data：Json对象，在 {@link com.github.theword.queqiao.tool.handle.HandleProtocolMessage } 中处理
 *
 * <p>echo：用于请求结果以及返回值
 */
public class BasePayload {
    /**
     * API名称
     */
    private String api;

    /**
     * Json对象
     */
    private JsonElement data;

    /**
     * 回声
     *
     * <p>如果指定了 echo 字段, 那么响应包也会同时包含一个 echo 字段, 它们会有相同的值
     */
    private String echo;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public JsonElement getData() {
        return data;
    }

    public void setData(JsonElement data) {
        this.data = data;
    }

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    public BasePayload() {
    }

    public BasePayload(String api, JsonElement data, String echo) {
        this.api = api;
        this.data = data;
        this.echo = echo;
    }
}
