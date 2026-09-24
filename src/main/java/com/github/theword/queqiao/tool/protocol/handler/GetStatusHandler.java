package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.protocol.handler.status.ServerStatusCollector;
import org.slf4j.Logger;

import java.util.Map;

public class GetStatusHandler extends AbstractProtocolHandler<EmptyPayload, Map<String, Object>> {

    public GetStatusHandler(Logger logger) {
        super(logger, EmptyPayload.class);
    }

    /**
     * 返回服务器状态快照
     *
     * <p><b>注意：这是潜在阻塞调用</b>——会执行一次 Minecraft Server List Ping
     * （socket 超时 3 秒）并采集 CPU/内存。它运行在调用方的连接读线程上，
     * 因此只会阻塞发起请求的那条连接，不影响其它连接。
     *
     * <p>{@link ServerStatusCollector} 内部有短 TTL 缓存，用于把突发请求收敛为一次采集。
     *
     * <p>日志级别为 debug：该接口可能被高频轮询，INFO 级别会造成日志刷屏。
     *
     * @param payload 空负载
     * @return 状态快照 Map
     */
    @Override
    protected Map<String, Object> handlePayload(EmptyPayload payload) {
        this.logger.debug("收到 get_status 请求，返回服务器状态快照");
        return ServerStatusCollector.collectStatusSnapshot();
    }
}
