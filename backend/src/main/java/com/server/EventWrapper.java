package com.server;

import java.util.Map;

public class EventWrapper {
    // private static final Map<String, Integer> typeMap = Map.of(
    //         "CHAT", Event.CHAT,
    //         "CANVAS", Event.CANVAS);

    private String type;
    private Event data;

    // No-arg constructor needed for deserialization
    public EventWrapper() {
    }

    public EventWrapper(Event e) {
        switch (e) {
            case Game.CanvasUpdate g -> type = "CANVAS";
            case Chat c -> type = "CHAT";
            default -> throw new IllegalArgumentException("Invalid `Event` type.");
        }
        data = e;
    }

    public Event unwrap() {
        return data;
    }

    public EventWrapper(String type, Event data) {
        this.type = type;
        this.data = data;
    }

    public String getId() {
        return data.getId();
    }

    public void setSequenceNumber(int value) {
        data.setSequenceNumber(value);
    }
    
    public int getSequenceNumber() {
        return data.getSequenceNumber();
    }

    public int compareTo(EventWrapper e) {
        return data.compareTo(e.data);
    }

}
