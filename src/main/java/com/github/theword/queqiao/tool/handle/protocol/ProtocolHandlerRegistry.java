package com.github.theword.queqiao.tool.handle.protocol;

import com.github.theword.queqiao.tool.exception.protocol.ProtocolHandlerRegistryException;

import java.util.HashMap;
import java.util.Map;

/**
 * 协议 API 处理器注册表。
 */
public class ProtocolHandlerRegistry {
    private final Map<String, ProtocolHandler> handlers = new HashMap<>();

    public ProtocolHandlerRegistry register(String api, ProtocolHandler handler) {
        if (api == null || api.isEmpty()) {
            throw ProtocolHandlerRegistryException.apiBlank();
        }
        if (handler == null) {
            throw ProtocolHandlerRegistryException.handlerNull(api);
        }
        if (handlers.containsKey(api)) {
            throw ProtocolHandlerRegistryException.duplicated(api);
        }
        handlers.put(api, handler);
        return this;
    }

    public ProtocolHandler get(String api) {
        return handlers.get(api);
    }
}
