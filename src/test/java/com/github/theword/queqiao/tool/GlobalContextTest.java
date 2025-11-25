package com.github.theword.queqiao.tool;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class GlobalContextTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson = GsonUtils.getGson();

    public JsonObject initMessagePrefixJsonObjectDemo(String messagePrefixText) {
        if (messagePrefixText == null || messagePrefixText.isEmpty()) {
            messagePrefixText = "[鹊桥]";
        }
        try {
            JsonElement element = gson.fromJson(messagePrefixText, JsonElement.class);
            if (element.isJsonObject()) {
                logger.info("消息前缀 {} 符合mc消息组件格式，将采用自定义风格的消息前缀", messagePrefixText);
                return element.getAsJsonObject();
            }
            logger.info("消息前缀 {} 不是合法的 JSON 对象，将使用默认风格的自定义文本前缀", messagePrefixText);

        } catch (JsonSyntaxException e) {
            logger.info("消息前缀 {} 未采用自定义风格，将使用默认风格的自定义文本前缀", messagePrefixText);
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("text", messagePrefixText);
        obj.addProperty("color", "yellow");
        return obj;
    }

    @Test
    public void testInitMessagePrefixJsonObject() {
        JsonObject jsonObject = initMessagePrefixJsonObjectDemo("[鹊桥]");
        logger.info(jsonObject.toString());
        assertEquals("yellow", jsonObject.get("color").getAsString());
        assertEquals("[鹊桥]", jsonObject.get("text").getAsString());
    }

    @Test
    public void testInitMessagePrefixJsonObject2() {
        JsonObject jsonObject = initMessagePrefixJsonObjectDemo("{\"text\":\"[鹊桥]\",\"color\":\"green\"}");
        logger.info(jsonObject.toString());
        assertEquals("green", jsonObject.get("color").getAsString());
        assertEquals("[鹊桥]", jsonObject.get("text").getAsString());
    }

}