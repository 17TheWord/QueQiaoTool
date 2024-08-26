package com.github.theword.queqiao.tool.payload.modle;

import com.github.theword.queqiao.tool.payload.modle.hover.CommonHoverItem;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;


class CommonHoverItemTest {

    @Test
    void testHoverItem() {
        Gson gson = new Gson();
        CommonHoverItem commonHoverItem = gson.fromJson("{\"id\": 1,\"count\": 1,\"tag\": \"tag\", \"key\": \"minecraft:dirt\"}", CommonHoverItem.class);
        System.out.println(commonHoverItem.getId());
        System.out.println(commonHoverItem.getCount());
        System.out.println(commonHoverItem.getTag());
        System.out.println(commonHoverItem.getKey());
    }

}