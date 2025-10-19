package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import org.junit.jupiter.api.Test;


class TitlePayloadTest {

    @Test
    void testSerializer() {
        String jsonData = "{\n" +
                "        \"title\": {\n" +
                "            \"text\": \"Title\",\n" +
                "            \"color\": \"aqua\"\n" +
                "        },\n" +
                "        \"subtitle\": {\n" +
                "            \"text\": \"Sub Title\"\n" +
                "        },\n" +
                "        \"fade_in\": 25,\n" +
                "        \"stay\": 70,\n" +
                "        \"fade_out\": 20\n" +
                "    }";
        TitlePayload titlePayload = GsonUtils.getGson().fromJson(jsonData, TitlePayload.class);
        System.out.println(titlePayload.getTitle());
        System.out.println(titlePayload.getSubtitle());
        System.out.println(titlePayload.getFadeIn());
        System.out.println(titlePayload.getStay());
        System.out.println(titlePayload.getFadeOut());
    }
}
