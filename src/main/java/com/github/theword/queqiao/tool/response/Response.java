package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response
 * <p>code：返回值</p>
 * <p>message：返回信息</p>
 * <p>data：数据</p>
 * <p>echo：请求的echo，从请求中获取</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private Integer code;
    @SerializedName("post_type")
    private String postType = "response";
    private ResponseEnum status;
    private String message;
    private Object data = null;
    private String echo;

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
     */
    public String getJson() {
        return GsonUtils.buildGson().toJson(this);
    }
}
