package com.github.theword.queqiao.tool.response;

import com.github.theword.queqiao.tool.utils.GsonUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    void testJsonData() {
        Response response = new Response(200, ResponseEnum.SUCCESS, "success", null, "echo123");
        assertEquals(200, response.getCode());
        assertEquals(ResponseEnum.SUCCESS, response.getStatus());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
        assertEquals("response", response.getPostType());
        assertEquals("echo123", response.getEcho());
        String json = GsonUtils.getGson().toJson(response);
        logger.info(json);
        assertTrue(json.contains("\"code\":200"));
        assertTrue(json.contains("\"post_type\":\"response\""));
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains("\"message\":\"success\""));
        assertTrue(json.contains("\"echo\":\"echo123\""));
    }

    @Test
    void testFactoryMethods() {
        Response r1 = Response.success("data", "echo");
        assertEquals(200, r1.getCode());
        assertEquals(ResponseEnum.SUCCESS, r1.getStatus());
        assertEquals("success", r1.getMessage());
        assertEquals("data", r1.getData());
        assertEquals("echo", r1.getEcho());
        Response r2 = Response.failed(404, "not found", "echo");
        assertEquals(404, r2.getCode());
        assertEquals(ResponseEnum.FAILED, r2.getStatus());
        assertEquals("not found", r2.getMessage());
        assertNull(r2.getData());
        assertEquals("echo", r2.getEcho());
    }
}
