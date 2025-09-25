package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;

/**
 * Response
 *
 * <p>code：返回值
 *
 * <p>message：返回信息
 *
 * <p>data：数据
 *
 * <p>echo：请求的echo，从请求中获取
 */
public class Response {
    private Integer code;

    @SerializedName("post_type")
    private String postType = "response";

    private ResponseEnum status;
    private String message;
    private Object data = null;
    private String echo;

    public Response() {
    }

    /**
     * 全参构造器
     *
     * @param code    HTTP 风格的返回码或状态码
     * @param status  响应状态枚举
     * @param message 响应信息
     * @param data    响应数据负载
     * @param echo    请求的 echo 值（回声）
     */
    public Response(Integer code, ResponseEnum status, String message, Object data, String echo) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
        this.echo = echo;
    }

    /**
     * 将响应对象序列化为 JSON 字符串，用于发送给客户端。
     *
     * @return 响应的 JSON 表示
     * @deprecated 请使用 {@link GsonUtils#buildGson()} 代替
     */
    @Deprecated
    public String getJson() {
        return GsonUtils.buildGson().toJson(this);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getPostType() {
        return postType;
    }

    public void setPostType(String postType) {
        this.postType = postType;
    }

    public ResponseEnum getStatus() {
        return status;
    }

    public void setStatus(ResponseEnum status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    public static Response success(Object data, String echo) {
        return new Response(200, ResponseEnum.SUCCESS, "success", data, echo);
    }

    public static Response success(String message, Object data, String echo) {
        return new Response(200, ResponseEnum.SUCCESS, message, data, echo);
    }

    public static Response failed(int code, String message, String echo) {
        return new Response(code, ResponseEnum.FAILED, message, null, echo);
    }

    public static Response failed(int code, String message, Object data, String echo) {
        return new Response(code, ResponseEnum.FAILED, message, data, echo);
    }

    // Convenience overloads for common uses
    public static Response success() {
        return success(null, null);
    }

    public static Response success(Object data) {
        return success(data, null);
    }

    public static Response successMessage(String message) {
        return success(message, null, null);
    }

    public static Response failed(int code, String message) {
        return failed(code, message, (Object) null, null);
    }

    public static Response failed(String message) {
        return failed(500, message, (Object) null, null);
    }
}
