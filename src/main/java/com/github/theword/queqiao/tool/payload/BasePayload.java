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
    private String api;
    private JsonElement data;
    private String echo;
}
