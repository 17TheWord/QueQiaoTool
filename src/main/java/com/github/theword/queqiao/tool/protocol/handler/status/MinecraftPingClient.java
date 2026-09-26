package com.github.theword.queqiao.tool.protocol.handler.status;

import com.github.theword.queqiao.tool.exception.status.MinecraftPingException;
import com.github.theword.queqiao.tool.utils.GsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class MinecraftPingClient {
    private static final int SOCKET_TIMEOUT_MILLIS = 3000;
    private static final int MAX_RESPONSE_PACKET_LENGTH = 1024 * 1024;
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
            long responseDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(SOCKET_TIMEOUT_MILLIS);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            sendHandshakePacket(outputStream, host, port);
            sendStatusRequestPacket(outputStream);

            int responsePacketLength = readVarInt(inputStream, socket, responseDeadlineNanos);
            if (responsePacketLength <= 0 || responsePacketLength > MAX_RESPONSE_PACKET_LENGTH) {
                throw MinecraftPingException.invalidPacketLength(responsePacketLength);
            }

            byte[] responsePacket = readFully(inputStream, socket, responseDeadlineNanos, responsePacketLength);
            ByteArrayInputStream packetInput = new ByteArrayInputStream(responsePacket);

            int packetId = readVarInt(packetInput);
            if (packetId != PACKET_ID_STATUS_RESPONSE) {
                throw MinecraftPingException.invalidPacketId(packetId);
            }

            int jsonLength = readVarInt(packetInput);
            if (jsonLength <= 0 || jsonLength != packetInput.available()) {
                throw MinecraftPingException.invalidJsonLength(jsonLength);
            }

            byte[] jsonBytes = readFully(packetInput, jsonLength);
            String json = decodeUtf8(jsonBytes);
            Map<String, Object> pingData;
            try {
                // 直接用全局 Gson 单例，不经 GlobalContext——本类无需依赖全局上下文
                JsonElement root = new JsonParser().parse(json);
                if (root == null || !root.isJsonObject()) {
                    throw MinecraftPingException.jsonParseFailed(null);
                }
                pingData = GsonUtils.getGson().fromJson(root, MAP_TYPE);
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
        int result = 0;
        for (int numRead = 0; numRead < MAX_VAR_INT_BYTES; numRead++) {
            int read = inputStream.read();
            if (read == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingVarInt();
            }
            if (numRead == MAX_VAR_INT_BYTES - 1 && (read & 0xF0) != 0) {
                throw MinecraftPingException.varIntOverflow();
            }
            int value = read & VAR_INT_SEGMENT_BITS;
            result |= value << (VAR_INT_BITS_PER_BYTE * numRead);
            if ((read & VAR_INT_CONTINUE_BIT) == 0) {
                return result;
            }
        }
        throw MinecraftPingException.varIntTooLong();
    }

    private int readVarInt(InputStream inputStream, Socket socket, long deadlineNanos)
            throws IOException, MinecraftPingException {
        int result = 0;
        for (int numRead = 0; numRead < MAX_VAR_INT_BYTES; numRead++) {
            setRemainingReadTimeout(socket, deadlineNanos);
            int read = inputStream.read();
            if (read == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingVarInt();
            }
            if (numRead == MAX_VAR_INT_BYTES - 1 && (read & 0xF0) != 0) {
                throw MinecraftPingException.varIntOverflow();
            }
            result |= (read & VAR_INT_SEGMENT_BITS) << (VAR_INT_BITS_PER_BYTE * numRead);
            if ((read & VAR_INT_CONTINUE_BIT) == 0) {
                return result;
            }
        }
        throw MinecraftPingException.varIntTooLong();
    }

    private byte[] readFully(InputStream inputStream, Socket socket, long deadlineNanos, int length)
            throws IOException, MinecraftPingException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            setRemainingReadTimeout(socket, deadlineNanos);
            int readCount = inputStream.read(data, offset, length - offset);
            if (readCount == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingResponse();
            }
            if (readCount == 0) {
                setRemainingReadTimeout(socket, deadlineNanos);
                int singleByte = inputStream.read();
                if (singleByte == INPUT_STREAM_EOF) {
                    throw MinecraftPingException.connectionClosedWhileReadingResponse();
                }
                data[offset++] = (byte) singleByte;
                continue;
            }
            offset += readCount;
        }
        return data;
    }

    private byte[] readFully(InputStream inputStream, int length) throws IOException, MinecraftPingException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int readCount = inputStream.read(data, offset, length - offset);
            if (readCount == INPUT_STREAM_EOF) {
                throw MinecraftPingException.connectionClosedWhileReadingResponse();
            }
            if (readCount == 0) {
                continue;
            }
            offset += readCount;
        }
        return data;
    }

    private void setRemainingReadTimeout(Socket socket, long deadlineNanos) throws IOException {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            throw new SocketTimeoutException("Minecraft Ping 响应读取超时");
        }
        long remainingMillis = (remainingNanos + 999_999L) / 1_000_000L;
        socket.setSoTimeout((int) Math.max(1L, Math.min(Integer.MAX_VALUE, remainingMillis)));
    }

    private String decodeUtf8(byte[] jsonBytes) throws MinecraftPingException {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(jsonBytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw MinecraftPingException.jsonParseFailed(e);
        }
    }
}
