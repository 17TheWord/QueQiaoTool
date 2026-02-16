package com.github.theword.queqiao.tool.utils;

import com.github.theword.queqiao.tool.GlobalContext;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class MinecraftPingClient {
    private static final int SOCKET_TIMEOUT_MILLIS = 3000;
    private static final int HANDSHAKE_PROTOCOL_VERSION = -1;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    Map<String, Object> fetchStatus(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT_MILLIS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            sendHandshakePacket(outputStream, host, port);
            sendStatusRequestPacket(outputStream);

            int responsePacketLength = readVarInt(inputStream);
            if (responsePacketLength <= 0) {
                throw new IOException("状态响应包长度非法: " + responsePacketLength);
            }

            int packetId = readVarInt(inputStream);
            if (packetId != 0x00) {
                throw new IOException("状态响应包 ID 非法: " + packetId);
            }

            int jsonLength = readVarInt(inputStream);
            if (jsonLength <= 0) {
                throw new IOException("状态响应 JSON 长度非法: " + jsonLength);
            }

            byte[] jsonBytes = readFully(inputStream, jsonLength);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            Map<String, Object> pingData = GlobalContext.getGson().fromJson(json, MAP_TYPE);
            if (pingData == null) {
                throw new IOException("状态响应 JSON 解析失败");
            }
            return pingData;
        }
    }

    private void sendHandshakePacket(OutputStream outputStream, String host, int port) throws IOException {
        ByteArrayOutputStream handshakeBody = new ByteArrayOutputStream();
        writeVarInt(handshakeBody, 0x00);
        writeVarInt(handshakeBody, HANDSHAKE_PROTOCOL_VERSION);
        writeString(handshakeBody, host);
        writeUnsignedShort(handshakeBody, port);
        writeVarInt(handshakeBody, 0x01);
        writePacket(outputStream, handshakeBody.toByteArray());
    }

    private void sendStatusRequestPacket(OutputStream outputStream) throws IOException {
        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        writeVarInt(requestBody, 0x00);
        writePacket(outputStream, requestBody.toByteArray());
    }

    private void writePacket(OutputStream outputStream, byte[] packetBody) throws IOException {
        ByteArrayOutputStream packet = new ByteArrayOutputStream();
        writeVarInt(packet, packetBody.length);
        packet.write(packetBody);
        outputStream.write(packet.toByteArray());
        outputStream.flush();
    }

    private void writeString(OutputStream outputStream, String text) throws IOException {
        byte[] value = text.getBytes(StandardCharsets.UTF_8);
        writeVarInt(outputStream, value.length);
        outputStream.write(value);
    }

    private void writeUnsignedShort(OutputStream outputStream, int value) throws IOException {
        outputStream.write((value >>> 8) & 0xFF);
        outputStream.write(value & 0xFF);
    }

    private void writeVarInt(OutputStream outputStream, int value) throws IOException {
        int current = value;
        while (true) {
            if ((current & 0xFFFFFF80) == 0) {
                outputStream.write(current);
                return;
            }
            outputStream.write((current & 0x7F) | 0x80);
            current >>>= 7;
        }
    }

    private int readVarInt(InputStream inputStream) throws IOException {
        int numRead = 0;
        int result = 0;
        int read;
        do {
            read = inputStream.read();
            if (read == -1) {
                throw new EOFException("读取 VarInt 时连接提前关闭");
            }
            int value = read & 0x7F;
            result |= value << (7 * numRead);
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt 过长");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private byte[] readFully(InputStream inputStream, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int readCount = inputStream.read(data, offset, length - offset);
            if (readCount == -1) {
                throw new EOFException("读取状态响应时连接提前关闭");
            }
            offset += readCount;
        }
        return data;
    }
}
