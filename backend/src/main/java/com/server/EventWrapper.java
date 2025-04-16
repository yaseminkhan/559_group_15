package com.server;

public class EventWrapper {
    public String type; // e.g., "CHAT", "CANVAS", "CLEAR"
    public Event data;

    // No-arg constructor needed for deserialization
    public EventWrapper() {}

    public EventWrapper(String type, Event data) {
        this.type = type;
        this.data = data;
    }
}
