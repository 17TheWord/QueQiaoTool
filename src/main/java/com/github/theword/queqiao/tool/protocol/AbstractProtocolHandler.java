package com.github.theword.queqiao.tool.protocol;

import com.github.theword.queqiao.tool.constant.ProtocolConstants;
import com.github.theword.queqiao.tool.exception.protocol.ProtocolException;
import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;

/**
 * 协议处理器抽象基类
 *
 * <p><b>线程安全约束（重要）</b>：处理器实例由 {@code ProtocolRouter} 在构造阶段创建一次，
 * 随后在多个连接、多个线程之间共享。因此实现<b>必须无状态</b>——
 * 不得持有任何随请求变化的字段。
 *
 * <p>全部输入应来自方法参数，输出通过返回值表达。
 * 违反该约束会引入静默的数据竞争：不同连接的请求会互相污染中间状态。
 *
 * @param <P> 负载类型
 * @param <R> 返回类型
 * @since 0.6.11
 */
public abstract class AbstractProtocolHandler<P, R> {
    private final Class<P> payloadType;

    protected AbstractProtocolHandler(Class<P> payloadType) {
        this.payloadType = payloadType;
    }

    public final R handle(JsonElement data) throws ProtocolException {
        if (payloadType == EmptyPayload.class) {
            return handlePayload(payloadType.cast(EmptyPayload.INSTANCE));
        }
        P payload;
        try {
            payload = GsonUtils.getGson().fromJson(data, payloadType);
        } catch (JsonParseException | IllegalStateException e) {
            throw ProtocolException.badRequest(ProtocolConstants.Message.PARSE_DATA_FAILED, data);
        }
        if (payload == null) {
            throw ProtocolException.badRequest(ProtocolConstants.Message.PARSE_DATA_FAILED, data);
        }
        return handlePayload(payload);
    }

    protected abstract R handlePayload(P payload) throws ProtocolException;
}
