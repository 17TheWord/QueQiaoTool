package com.github.theword.queqiao.tool.websocket;

/**
 * 重连原因
 *
 * <p>所有重连请求都必须携带原因，统一进入 {@link WsClient} 的重连 pipeline。
 *
 * <p>说明：不存在 "CONNECTION_ERROR" 这一原因。
 * {@link WsClient#onError(Exception)} 只记录错误、不安排重连——
 * Java-WebSocket 在连接失败与异常关闭时都会随后触发 {@code onClose}，
 * 由 {@code onClose} 作为自动重连的唯一入口，避免一次故障被安排两次重连。
 *
 * @since 0.6.11
 */
public enum ReconnectReason {

    /**
     * 连接层关闭（含连接失败、异常关闭、心跳超时被库关闭）
     */
    REMOTE_CLOSE,

    /**
     * 手动触发（管理员命令）
     */
    MANUAL
}
