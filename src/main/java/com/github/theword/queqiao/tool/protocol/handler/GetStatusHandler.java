package com.github.theword.queqiao.tool.protocol.handler;

import com.github.theword.queqiao.tool.payload.EmptyPayload;
import com.github.theword.queqiao.tool.handle.HandleApiService;
import com.github.theword.queqiao.tool.protocol.AbstractProtocolHandler;
import com.github.theword.queqiao.tool.protocol.handler.status.ServerStatusCollector;
import org.slf4j.Logger;

import java.util.Map;

public class GetStatusHandler extends AbstractProtocolHandler<EmptyPayload, Map<String, Object>> {

    public GetStatusHandler(Logger logger, HandleApiService handleApiService) {
        super(logger, handleApiService, EmptyPayload.class);
    }

    /**
     * 返回服务器状态快照
     *
     * <p>Runtime 启动后由后台任务按配置间隔采集完整快照。本处理器只读取最近一次已发布的快照，
     * 不会在连接处理线程中执行 Minecraft Server List Ping。
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
