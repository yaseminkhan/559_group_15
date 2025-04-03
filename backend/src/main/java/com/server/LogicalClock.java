package com.server;

public class LogicalClock {
    private int time = 0;

    public synchronized int getTime() {
        return time;
    }

    // public synchronized void tick() {
    //     time++;
    // }

    public synchronized void update(int incomingTimestamp) {
        time = Math.max(time, incomingTimestamp) + 1;
    }

    public synchronized int getAndUpdate(int incomingTimestamp) {
        update(incomingTimestamp);
        return time;
    }
}