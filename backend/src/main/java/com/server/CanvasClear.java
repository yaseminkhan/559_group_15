package com.server;

public class CanvasClear extends Event {
    public long timestamp;

    public CanvasClear() {
        this.timestamp = System.currentTimeMillis();
    }

    public CanvasClear(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
