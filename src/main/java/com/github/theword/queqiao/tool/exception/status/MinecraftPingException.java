package com.github.theword.queqiao.tool.exception.status;

public class MinecraftPingException extends StatusException {
    public MinecraftPingException(String message) {
        super(message);
    }

    public MinecraftPingException(String message, Throwable cause) {
        super(message, cause);
    }

    public static MinecraftPingException connectionFailed(String host, int port, Throwable cause) {
        return new MinecraftPingException("Minecraft Server List Ping 连接失败: " + host + ":" + port, cause);
    }

    public static MinecraftPingException invalidPacketLength(int length) {
        return new MinecraftPingException("状态响应包长度非法: " + length);
    }

    public static MinecraftPingException invalidPacketId(int packetId) {
        return new MinecraftPingException("状态响应包 ID 非法: " + packetId);
    }

    public static MinecraftPingException invalidJsonLength(int length) {
        return new MinecraftPingException("状态响应 JSON 长度非法: " + length);
    }

    public static MinecraftPingException jsonParseFailed(Throwable cause) {
        return new MinecraftPingException("状态响应 JSON 解析失败", cause);
    }

    public static MinecraftPingException connectionClosedWhileReadingVarInt() {
        return new MinecraftPingException("读取 VarInt 时连接提前关闭");
    }

    public static MinecraftPingException varIntTooLong() {
        return new MinecraftPingException("VarInt 过长");
    }

    public static MinecraftPingException connectionClosedWhileReadingResponse() {
        return new MinecraftPingException("读取状态响应时连接提前关闭");
    }

    public static MinecraftPingException ioFailed(String message, Throwable cause) {
        return new MinecraftPingException(message, cause);
    }
}
