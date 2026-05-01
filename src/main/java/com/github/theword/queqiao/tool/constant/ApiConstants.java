package com.github.theword.queqiao.tool.constant;

/**
 * 协议 API 相关常量集合。
 */
public final class ApiConstants {
    private ApiConstants() {
    }

    /**
     * 协议 API 名称。
     */
    public static final class Api {
        public static final String BROADCAST = "broadcast";
        public static final String SEND_MSG = "send_msg";
        public static final String SEND_TITLE = "send_title";
        public static final String SEND_ACTIONBAR = "send_actionbar";
        public static final String SEND_PRIVATE_MSG = "send_private_msg";
        public static final String SEND_COMMAND = "send_command";
        public static final String SEND_RCON_COMMAND = "send_rcon_command";
        public static final String GET_STATUS = "get_status";

        private Api() {
        }
    }

    /**
     * 协议响应码。
     */
    public static final class Code {
        public static final int BAD_REQUEST = 400;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;

        private Code() {
        }
    }

    /**
     * 协议响应消息。
     */
    public static final class Message {
        public static final String UNKNOWN_API = "未知的API：";
        public static final String PARSE_MESSAGE_FAILED = "解析消息失败";
        public static final String SEND_COMMAND_UNSUPPORTED = "%s is not supported now";
        public static final String TITLE_AND_SUBTITLE_EMPTY = "Title and Subtitle cannot both be null";

        private Message() {
        }
    }
}
