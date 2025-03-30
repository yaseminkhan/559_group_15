package com.server;

public abstract class Event implements Comparable<Event> {

    protected int sequenceNo;  
    protected String id;      

    public Event() {}

    public String getId() {
        return id;
    }

    public void setId(String val) {
        id = val;
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }

    public void setSequenceNumber(int val) {
        sequenceNo = val;
    }

    @Override
    public int compareTo(Event e) {
        return Integer.compare(this.sequenceNo, e.sequenceNo);
    }
}