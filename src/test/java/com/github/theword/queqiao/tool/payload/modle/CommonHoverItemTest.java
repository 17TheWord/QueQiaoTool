package com.github.theword.queqiao.tool.payload.modle;

import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverItem;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class CommonHoverItemTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testHoverItem() {
        Gson gson = new Gson();
        CommonHoverItem commonHoverItem = gson.fromJson("{\"id\": 1,\"count\": 1,\"tag\": \"tag\", \"key\": \"minecraft:dirt\"}", CommonHoverItem.class);
        logger.info(commonHoverItem.getId());
        logger.info("{}", commonHoverItem.getCount());
        logger.info(commonHoverItem.getTag());
        logger.info(commonHoverItem.getKey());
    }

}