package com.github.theword.queqiao.tool.payload;

import com.google.gson.JsonElement;
import lombok.Data;

/**
 * BasePayload
 * <p>api：API名称</p>
 * <p>data：Json对象，在 {@link com.github.theword.queqiao.tool.handle.HandleProtocolMessage } 中处理</p>
 * <p>echo：用于请求结果以及返回值</p>
 */
@Data
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
     * <p>如果指定了 echo 字段, 那么响应包也会同时包含一个 echo 字段, 它们会有相同的值</p>
     */
    private String echo;
}
