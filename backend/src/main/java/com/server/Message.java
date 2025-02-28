package com.server;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {

    public enum MessageType {
        STROKE,
        CHAT,
        STATE,
    };

    public record Stroke(int id, int x, int y, double timestamp) {
        public Stroke(ByteBuffer buf) {
            this(
                    buf.getInt(1),
                    buf.getInt(5),
                    buf.getInt(9),
                    buf.getDouble(13));
        }

        public Stroke(byte[] msg) {
            this(ByteBuffer.wrap(msg));
        }
    }

    public record Chat(int id, double timestamp, String data) {
        public Chat(ByteBuffer buf) {
            this(buf.getInt(1), buf.getDouble(5),
                    new String(Arrays.copyOfRange(buf.array(), 13, buf.array().length)));
        }

        public Chat(byte[] msg) {
            this(ByteBuffer.wrap(msg));
        }
    }
}
