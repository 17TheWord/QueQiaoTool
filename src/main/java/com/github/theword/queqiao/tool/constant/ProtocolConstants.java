package com.github.theword.queqiao.tool.constant;

public final class ProtocolConstants {
    private ProtocolConstants() {
    }

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

    public static final class Message {
        public static final String FAILED = "failed";
        public static final String TITLE_AND_SUBTITLE_EMPTY = "Title and Subtitle cannot both be null";
        public static final String SEND_COMMAND_UNSUPPORTED = "send_command is not supported now";
        public static final String PARSE_MESSAGE_FAILED = "解析消息失败";
        public static final String PARSE_DATA_FAILED = "解析请求数据失败";

        private Message() {
        }
    }

    public static final class Status {
        public static final int BAD_REQUEST = 400;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;

        private Status() {
        }
    }
}
