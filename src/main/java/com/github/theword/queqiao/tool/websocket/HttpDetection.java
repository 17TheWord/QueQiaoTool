package com.github.theword.queqiao.tool.websocket;

/**
 * HTTP/WebSocket 首包探测工具。
 */
final class HttpDetection {

    private static final String[] HTTP_METHODS = new String[]{"GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE ", "CONNECT "
    };

    private HttpDetection() {
    }

    static boolean isHttpRequest(String preview) {
        for (String method : HTTP_METHODS) {
            if (preview.regionMatches(true, 0, method, 0, method.length())) {
                return true;
            }
        }
        return false;
    }

    static boolean isWebSocketUpgrade(String preview) {
        return containsIgnoreCase(preview, "upgrade: websocket");
    }

    static boolean hasFullHeaders(String preview, int readableBytes, int maxDetectBytes) {
        return preview.contains("\r\n\r\n") || readableBytes >= maxDetectBytes;
    }

    private static boolean containsIgnoreCase(String text, String token) {
        int max = text.length() - token.length();
        for (int i = 0; i <= max; i++) {
            if (text.regionMatches(true, i, token, 0, token.length())) {
                return true;
            }
        }
        return false;
    }
}
