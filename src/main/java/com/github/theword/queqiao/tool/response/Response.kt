package com.github.theword.queqiao.tool.response;

import com.google.gson.Gson;
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

    public Response(Integer code, ResponseEnum status, String message, Object data, String echo) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.data = data;
        this.echo = echo;
    }

    public String getJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
