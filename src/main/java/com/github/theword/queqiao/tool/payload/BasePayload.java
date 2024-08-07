package com.github.theword.queqiao.tool.payload;

import com.google.gson.JsonElement;
import lombok.Data;

@Data
public class BasePayload {

    private String api;

    private JsonElement data;

}
