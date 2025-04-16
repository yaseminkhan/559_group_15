package com.server;

// Abstract base class for all event types (e.g., chat messages, canvas updates)
public abstract class Event implements Comparable<Event> {

    protected int sequenceNumber;  // Lamport timestamp for ordering events
    protected String id;           // Sender ID for tie-breaking

    public Event() {}

    // Getter and setter for the event sender or event ID
    public String getId() {
        return id;
    }

    public void setId(String val) {
        id = val;
    }

    // Getter and setter for the Lamport sequence number
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int val) {
        sequenceNumber = val;
    }

    // Define ordering of events: first by sequence number, then by ID 
    @Override
    public int compareTo(Event e) {
        int cmp = Integer.compare(this.sequenceNumber, e.sequenceNumber);
        if (cmp != 0) return cmp;
        return this.id.compareTo(e.id); // Ensures consistent order even if timestamps match
    }
}