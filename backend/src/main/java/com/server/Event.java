package com.server;

public abstract class Event implements Comparable<Event> {

    // public static final int CHAT = 0;
    // public static final int CANVAS = 1;

    protected String id = ""; // Default unused.
    protected int sequenceNo = -1; // Default unused.

    // Empty constructor for serialization.
    public Event() {
    }

    // public abstract int getType();

    public int compareTo(Event e) {
        if (e.getSequenceNumber() == getSequenceNumber()) {
            return getId().compareTo(e.getId());
        }
        return Integer.compare(getSequenceNumber(), e.getSequenceNumber());
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        id = value;
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }

    public void setSequenceNumber(int value) {
        sequenceNo = value;
    }

}
