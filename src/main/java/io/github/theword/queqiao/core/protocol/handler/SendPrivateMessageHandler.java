package io.github.theword.queqiao.core.protocol.handler;

import io.github.theword.queqiao.core.handle.HandleApiService;
import io.github.theword.queqiao.core.exception.protocol.ProtocolException;
import io.github.theword.queqiao.core.payload.PrivateMessagePayload;
import io.github.theword.queqiao.core.protocol.AbstractProtocolHandler;
import io.github.theword.queqiao.core.response.PrivateMessageResponse;
import org.slf4j.Logger;

public class SendPrivateMessageHandler extends AbstractProtocolHandler<PrivateMessagePayload, PrivateMessageResponse> {

    public SendPrivateMessageHandler(Logger logger, HandleApiService handleApiService) {
        super(logger, handleApiService, PrivateMessagePayload.class);
    }

    /**
     * 处理私聊请求
     *
     * <p>校验规则：{@code nickname} 与 {@code uuid} 至少要有一个有效。
     * {@code nickname} 使用 {@code trim()} 后判空，因此纯空白字符串（如 {@code "   "}）视为未提供。
     *
     * <p>两者同时提供时的优先级由平台实现（{@code HandleApiService#handleSendPrivateMessage}）决定，
     * 本层不做取舍、原样传递——因为只有平台实现才知道本地玩家列表中哪些字段可靠。
     *
     * @param payload 私聊负载
     * @return 平台实现返回的响应
     * @throws ProtocolException 目标玩家信息缺失时抛出 400
     */
    @Override
    protected PrivateMessageResponse handlePayload(PrivateMessagePayload payload) throws ProtocolException {
        String nickname = payload.getNickname();
        String normalizedNickname = nickname == null ? null : nickname.trim();

        boolean nicknameMissing = normalizedNickname == null || normalizedNickname.isEmpty();
        if (nicknameMissing && payload.getUuid() == null) {
            PrivateMessageResponse response = PrivateMessageResponse.playerIsNull();
            throw ProtocolException.badRequest(response.getMessage(), response);
        }

        return this.handleApiService.handleSendPrivateMessage(normalizedNickname, payload.getUuid(), payload.getMessage());
    }
}
