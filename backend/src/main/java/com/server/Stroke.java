package com.server;

import java.nio.ByteBuffer;

public record Stroke(int id, int color, int x, int y, double timestamp) {
    public Stroke(ByteBuffer buf) {
        this(
                buf.getInt(1),
                buf.getInt(5),
                buf.getInt(9),
                buf.getInt(13),
                buf.getDouble(17));
    }

    public Stroke(byte[] msg) {
        this(ByteBuffer.wrap(msg));
    }
}
