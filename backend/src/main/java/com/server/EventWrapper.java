package com.server;

public class EventWrapper implements Comparable<EventWrapper> {
    public String type; // e.g., "CHAT", "CANVAS"
    public Event data;

    // No-arg constructor needed for deserialization
    public EventWrapper() {}

    public EventWrapper(String type, Event data) {
        this.type = type;
        this.data = data;
    }

    public int getSequenceNumber() {
        return data.getSequenceNumber();
    }

    public void setSequenceNumber(int value) {
        data.setSequenceNumber(value);
    }

    public int compareTo(EventWrapper e) {
        return data.compareTo(e.data);
    }
}
