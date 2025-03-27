package com.server;

public class LogicalClock {
    private int time;

    public LogicalClock() {
        time = 0;
    }

    public synchronized void setTime(int value) {
        if (value < time)
            throw new IllegalArgumentException("Logical time is monotonic.");
        time = value;
    }

    public synchronized int getTime() {
        return time;
    }

    public synchronized void tick() {
        ++time;
    }

    public synchronized void update(Event e) {
        var m = e.getMessage();
        switch (e.getType()) {
            case "send":
                tick();
                m.setSequenceNumber(time);
                break;
            case "receive":
                m.setSequenceNumber(Math.max(time, m.getSequenceNumber()) + 1);
                tick();
                break;
            default:
                tick();
        }
        e.setSequenceNumber(time);
    }
}
