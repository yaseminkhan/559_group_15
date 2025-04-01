package com.server;

public abstract class Event implements Comparable<Event> {

    protected int sequenceNumber;  
    protected String id;      

    public Event() {}

    public String getId() {
        return id;
    }

    public void setId(String val) {
        id = val;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int val) {
        sequenceNumber = val;
    }

    @Override
    public int compareTo(Event e) {
        int cmp = Integer.compare(this.sequenceNumber, e.sequenceNumber);
        if (cmp != 0) return cmp;
        return this.id.compareTo(e.id); // tie-breaker using UUID
    }
}