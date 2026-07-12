package com.github.theword.queqiao.tool.protocol.handler.status;

import com.github.theword.queqiao.tool.GlobalContext;
import com.github.theword.queqiao.tool.exception.status.MinecraftPingException;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
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
    private static final int PACKET_ID_HANDSHAKE = 0x00;
    private static final int PACKET_ID_STATUS_REQUEST = 0x00;
    private static final int PACKET_ID_STATUS_RESPONSE = 0x00;
    private static final int NEXT_STATE_STATUS = 0x01;
    private static final int VAR_INT_SEGMENT_BITS = 0x7F;
    private static final int VAR_INT_CONTINUE_BIT = 0x80;
    private static final int VAR_INT_REMAINING_BITS = 0xFFFFFF80;
    private static final int VAR_INT_BITS_PER_BYTE = 7;
    private static final int MAX_VAR_INT_BYTES = 5;
    private static final int UNSIGNED_BYTE_MASK = 0xFF;
    private static final int UNSIGNED_SHORT_HIGH_BYTE_SHIFT = 8;
    private static final int INPUT_STREAM_EOF = -1;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    MinecraftPingResponse fetchStatus(String host, int port) throws MinecraftPingException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), SOCKET_TIMEOUT_MILLIS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            sendHandshakePacket(outputStream, host, port);
            sendStatusRequestPacket(outputStream);

            int responsePacketLength = readVarInt(inputStream);
            if (responsePacketLength <= 0) {
                throw MinecraftPingException.invalidPacketLength(responsePacketLength);
            }

            int packetId = readVarInt(inputStream);
            if (packetId != PACKET_ID_STATUS_RESPONSE) {
                throw MinecraftPingException.invalidPacketId(packetId);
            }

            int jsonLength = readVarInt(inputStream);
            if (jsonLength <= 0) {
                throw MinecraftPingException.invalidJsonLength(jsonLength);
            }

            byte[] jsonBytes = readFully(inputStream, jsonLength);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            Map<String, Object> pingData;
            try {
                pingData = GlobalContext.getGson().fromJson(json, MAP_TYPE);
            } catch (JsonParseException | IllegalStateException e) {
                throw MinecraftPingException.jsonParseFailed(e);
            }
            if (pingData == null) {
                throw MinecraftPingException.jsonParseFailed(null);
            }
            return MinecraftPingResponse.fromRawData(pingData);
        } catch (IOException e) {
            throw MinecraftPingException.connectionFailed(host, port, e);
        }
    }

    private void sendHandshakePacket(OutputStream outputStream, String host, int port) throws IOException {
        ByteArrayOutputStream handshakeBody = new ByteArrayOutputStream();
        writeVarInt(handshakeBody, PACKET_ID_HANDSHAKE);
        writeVarInt(handshakeBody, HANDSHAKE_PROTOCOL_VERSION);
        writeString(handshakeBody, host);
        writeUnsignedShort(handshakeBody, port);
        writeVarInt(handshakeBody, NEXT_STATE_STATUS);
        writePacket(outputStream, handshakeBody.toByteArray());
    }

    private void sendStatusRequestPacket(OutputStream outputStream) throws IOException {
        ByteArrayOutputStream requestBody = new ByteArrayOutputStream();
        writeVarInt(requestBody, PACKET_ID_STATUS_REQUEST);
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
        outputStream.write((value >>> UNSIGNED_SHORT_HIGH_BYTE_SHIFT) & UNSIGNED_BYTE_MASK);
        outputStream.write(value & UNSIGNED_BYTE_MASK);
    }

    private void writeVarInt(OutputStream outputStream, int value) throws IOException {
        int current = value;
        while (true) {
            if ((current & VAR_INT_REMAINING_BITS) == 0) {
                outputStream.write(current);
                return;
            }
            outputStream.write((current & VAR_INT_SEGMENT_BITS) | VAR_INT_CONTINUE_BIT);
            current >>>= VAR_INT_BITS_PER_BYTE;
        }
    }

    private int readVarInt(InputStream inputStream) throws IOException, MinecraftPingException {
        int numRead = 0;
        int result = 0;
        int read;
        do {
            read = inputStream.read();
            if (read == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingVarInt();
            }
            int value = read & VAR_INT_SEGMENT_BITS;
            result |= value << (VAR_INT_BITS_PER_BYTE * numRead);
            numRead++;
            if (numRead > MAX_VAR_INT_BYTES) {
                throw MinecraftPingException.varIntTooLong();
            }
        } while ((read & VAR_INT_CONTINUE_BIT) != 0);
        return result;
    }

    private byte[] readFully(InputStream inputStream, int length) throws IOException, MinecraftPingException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int readCount = inputStream.read(data, offset, length - offset);
            if (readCount == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingResponse();
            }
            offset += readCount;
        }
        return data;
    }
}
