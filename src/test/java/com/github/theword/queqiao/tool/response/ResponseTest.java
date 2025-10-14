package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ResponseTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void getJson() {
        Response response = new Response(200, ResponseEnum.SUCCESS, "success", null, null);
        logger.info(GsonUtils.getGson().toJson(response));
    }
}
