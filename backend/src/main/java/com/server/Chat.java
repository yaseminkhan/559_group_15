package com.server;

import java.nio.ByteBuffer;
import java.util.Arrays;

public record Chat(int id, double timestamp, String data) {
    public Chat(ByteBuffer buf) {
        this(buf.getInt(1), buf.getDouble(5),
                new String(Arrays.copyOfRange(buf.array(), 13, buf.array().length)));
    }

    public Chat(byte[] msg) {
        this(ByteBuffer.wrap(msg));
    }
}
