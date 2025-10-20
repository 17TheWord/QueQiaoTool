package com.github.theword.queqiao.tool.payload;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class TitlePayloadTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testAllTitle() {
        String jsonData = "{\"title\":{\"text\":\"Title\",\"color\":\"aqua\"},\"subtitle\":{\"text\":\"Sub Title\"},\"fade_in\":25,\"stay\":70,\"fade_out\":20}";
        TitlePayload titlePayload = GsonUtils.getGson().fromJson(jsonData, TitlePayload.class);
        assertNotNull(titlePayload);
        JsonElement title = titlePayload.getTitle();
        assertNotNull(title);
        assertTrue(title.isJsonObject());
        JsonObject titleObj = title.getAsJsonObject();
        assertEquals("Title", titleObj.get("text").getAsString());
        assertEquals("aqua", titleObj.get("color").getAsString());
        JsonElement subtitle = titlePayload.getSubtitle();
        assertNotNull(subtitle);
        assertTrue(subtitle.isJsonObject());
        JsonObject subtitleObj = subtitle.getAsJsonObject();
        assertEquals("Sub Title", subtitleObj.get("text").getAsString());
        assertEquals(25, titlePayload.getFadeIn());
        assertEquals(70, titlePayload.getStay());
        assertEquals(20, titlePayload.getFadeOut());
    }

    @Test
    void testTitleOnly() {
        String jsonData = "{\"title\":{\"text\":\"Only Title\"}}";
        TitlePayload titlePayload = GsonUtils.getGson().fromJson(jsonData, TitlePayload.class);
        assertNotNull(titlePayload);
        assertNotNull(titlePayload.getTitle());
        assertFalse(titlePayload.getTitle().isJsonNull());
        assertNull(titlePayload.getSubtitle());
    }

    @Test
    void testSubtitleOnly() {
        String jsonData = "{\"subtitle\":{\"text\":\"Only Subtitle\"}}";
        TitlePayload titlePayload = GsonUtils.getGson().fromJson(jsonData, TitlePayload.class);
        assertNotNull(titlePayload);
        assertNull(titlePayload.getTitle());
        assertNotNull(titlePayload.getSubtitle());
        assertFalse(titlePayload.getSubtitle().isJsonNull());
    }
}
