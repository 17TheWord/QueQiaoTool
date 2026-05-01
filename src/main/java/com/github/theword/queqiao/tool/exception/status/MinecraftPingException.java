package com.github.theword.queqiao.tool.exception.status;

import com.github.theword.queqiao.tool.exception.QueQiaoToolException;

public class MinecraftPingException extends QueQiaoToolException {

    private final Reason reason;

    private MinecraftPingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    private MinecraftPingException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    private static MinecraftPingException of(Reason reason, String message) {
        return new MinecraftPingException(reason, message);
    }

    private static MinecraftPingException of(Reason reason, String message, Throwable cause) {
        return new MinecraftPingException(reason, message, cause);
    }

    public static MinecraftPingException invalidPacketLength(int length) {
        return of(Reason.INVALID_PACKET_LENGTH, "状态响应包长度非法: " + length);
    }

    public static MinecraftPingException invalidPacketId(int packetId) {
        return of(Reason.INVALID_PACKET_ID, "状态响应包 ID 非法: " + packetId);
    }

    public static MinecraftPingException invalidJsonLength(int length) {
        return of(Reason.INVALID_JSON_LENGTH, "状态响应 JSON 长度非法: " + length);
    }

    public static MinecraftPingException jsonParseFailed() {
        return of(Reason.JSON_PARSE_FAILED, "状态响应 JSON 解析失败");
    }

    public static MinecraftPingException connectionClosed(String message) {
        return of(Reason.CONNECTION_CLOSED, message);
    }

    public static MinecraftPingException varIntTooLong() {
        return of(Reason.VAR_INT_TOO_LONG, "VarInt 过长");
    }

    public static MinecraftPingException ioFailed(Throwable cause) {
        return of(Reason.IO_FAILED, "Minecraft Server List Ping IO 失败", cause);
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        INVALID_PACKET_LENGTH,
        INVALID_PACKET_ID,
        INVALID_JSON_LENGTH,
        JSON_PARSE_FAILED,
        CONNECTION_CLOSED,
        VAR_INT_TOO_LONG,
        IO_FAILED
    }
}
