package com.github.theword.queqiao.tool.handle;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.response.Response;
import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 协议分发矩阵测试
 *
 * <p>覆盖 WS-B2（状态码语义）、WS-B3（错误响应不回传原始请求体）、
 * WS-B5（Payload 输入校验）以及 B4 解耦后新增可测的"未知 api"分支。
 *
 * <p>统一走 {@code handleHttpJson} 入口：它与 WebSocket 入口共用同一套
 * 解析与路由逻辑，但不需要构造 {@code WebSocket}，因此测试无网络依赖。
 */
class ProtocolDispatchTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolDispatchTest.class);
    private static final Gson GSON = new Gson();

    private static final int BAD_REQUEST = ProtocolConstants.Status.BAD_REQUEST;
    private static final int NOT_FOUND = ProtocolConstants.Status.NOT_FOUND;
    private static final int INTERNAL_ERROR = ProtocolConstants.Status.INTERNAL_ERROR;

    /**
     * 分发一个请求并解析回 {@link Response}
     */
    private static Response dispatch(String rawJson) {
        String responseJson = new HandleProtocolMessage(LOGGER, GSON).handleHttpJson(rawJson);
        Response response = GSON.fromJson(responseJson, Response.class);
        assertNotNull(response, "分发结果不应为 null，原始响应=" + responseJson);
        assertNotNull(response.getCode(), "响应应带状态码，原始响应=" + responseJson);
        return response;
    }

    // ------------------------------------------------------------------
    // 解析阶段：调用方错误一律 400，不再伪装成 500
    // ------------------------------------------------------------------

    @Test
    @DisplayName("非法 JSON 请求体返回 400")
    void malformedJsonReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("{\"api\":\"broadcast\"").getCode().intValue());
    }

    @Test
    @DisplayName("JSON 数组作为请求体返回 400")
    void jsonArrayBodyReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("[1,2,3]").getCode().intValue());
    }

    @Test
    @DisplayName("字面量 null 请求体返回 400")
    void nullLiteralBodyReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("null").getCode().intValue());
    }

    @Test
    @DisplayName("缺少 api 字段返回 400")
    void missingApiReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("{\"data\":{}}").getCode().intValue());
    }

    @Test
    @DisplayName("api 为纯空白返回 400")
    void blankApiReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("{\"api\":\"   \"}").getCode().intValue());
    }

    // ------------------------------------------------------------------
    // 路由阶段
    // ------------------------------------------------------------------

    @Test
    @DisplayName("未注册的 api 返回 404")
    void unknownApiReturnsNotFound() {
        assertEquals(NOT_FOUND, dispatch("{\"api\":\"no_such_api\"}").getCode().intValue());
    }

    @Test
    @DisplayName("已注册但不受支持的 api 返回 500")
    void unsupportedApiReturnsInternalError() {
        assertEquals(INTERNAL_ERROR, dispatch("{\"api\":\"send_command\"}").getCode().intValue());
    }

    @Test
    @DisplayName("send_title 缺少 title 与 subtitle 返回 400")
    void sendTitleWithoutContentReturnsBadRequest() {
        assertEquals(BAD_REQUEST, dispatch("{\"api\":\"send_title\",\"data\":{}}").getCode().intValue());
    }

    // ------------------------------------------------------------------
    // WS-B5 输入校验
    // ------------------------------------------------------------------

    @Test
    @DisplayName("send_title 的 fade_in 为负数返回 400")
    void sendTitleWithNegativeDurationReturnsBadRequest() {
        String body = "{\"api\":\"send_title\",\"data\":{\"title\":{\"text\":\"hi\"},\"fade_in\":-1}}";
        Response response = dispatch(body);

        assertEquals(BAD_REQUEST, response.getCode().intValue());
        assertEquals(ProtocolConstants.Message.TITLE_DURATION_NEGATIVE, response.getMessage());
    }

    @Test
    @DisplayName("send_title 的 stay 超出上限返回 400")
    void sendTitleWithTooLargeDurationReturnsBadRequest() {
        String body = "{\"api\":\"send_title\",\"data\":{\"title\":{\"text\":\"hi\"},\"stay\":999999}}";
        Response response = dispatch(body);

        assertEquals(BAD_REQUEST, response.getCode().intValue());
        assertEquals(ProtocolConstants.Message.TITLE_DURATION_TOO_LARGE, response.getMessage());
    }

    @Test
    @DisplayName("send_title 的合法时长通过校验（进入平台调用前即返回，不触发 400）")
    void sendTitleWithValidDurationPassesValidation() {
        String body = "{\"api\":\"send_title\",\"data\":{\"title\":{\"text\":\"hi\"},\"fade_in\":10,\"stay\":70,\"fade_out\":20}}";
        Response response = dispatch(body);

        // 未初始化 HandleApiService，因此会落到 500；关键是"不是 400"，
        // 说明时长校验已放行、异常来自平台调用缺失而非参数校验
        assertFalse(response.getCode().intValue() == BAD_REQUEST, "合法时长不应被判为参数错误");
    }

    @Test
    @DisplayName("send_private_msg 的 nickname 为纯空白返回 400")
    void sendPrivateMessageWithBlankNicknameReturnsBadRequest() {
        String body = "{\"api\":\"send_private_msg\",\"data\":{\"nickname\":\"   \",\"message\":\"hi\"}}";
        assertEquals(BAD_REQUEST, dispatch(body).getCode().intValue());
    }

    @Test
    @DisplayName("send_rcon_command 的命令为空白返回 400")
    void sendRconCommandWithBlankCommandReturnsBadRequest() {
        String body = "{\"api\":\"send_rcon_command\",\"data\":{\"command\":\"   \"}}";
        Response response = dispatch(body);

        assertEquals(BAD_REQUEST, response.getCode().intValue());
        assertEquals(ProtocolConstants.Message.RCON_COMMAND_EMPTY, response.getMessage());
    }

    // ------------------------------------------------------------------
    // 响应契约
    // ------------------------------------------------------------------

    @Test
    @DisplayName("echo 原样回传")
    void echoIsReturnedAsIs() {
        assertEquals("req-42", dispatch("{\"api\":\"no_such_api\",\"echo\":\"req-42\"}").getEcho());
    }

    @Test
    @DisplayName("api 原样回传")
    void apiIsReturnedAsIs() {
        assertEquals("no_such_api", dispatch("{\"api\":\"no_such_api\"}").getApi());
    }

    @Test
    @DisplayName("post_type 固定为 response")
    void postTypeIsAlwaysResponse() {
        assertEquals("response", dispatch("{\"api\":\"no_such_api\"}").getPostType());
    }

    /**
     * WS-B3 回归：错误响应不得回传原始请求体
     *
     * <p>修复前解析失败会把 {@code rawJsonMessage} 塞进 {@code data} 原样返回，
     * 既放大了响应体积，也可能把敏感内容回显给调用方。
     */
    @Test
    @DisplayName("解析失败时响应不回传原始请求体（WS-B3 回归）")
    void parseFailureResponseDoesNotEchoRawRequest() {
        String marker = "s3cr3t-payload-marker";
        String malformedBody = "{\"api\":\"broadcast\",\"data\":{\"note\":\"" + marker + "\"";

        String responseJson = new HandleProtocolMessage(LOGGER, GSON).handleHttpJson(malformedBody);

        assertFalse(responseJson.contains(marker), "响应不得包含原始请求内容：" + responseJson);
        assertFalse(responseJson.contains("rawJsonMessage"), "响应不得包含 rawJsonMessage 字段：" + responseJson);
    }
}
