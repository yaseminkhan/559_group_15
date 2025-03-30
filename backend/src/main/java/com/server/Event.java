package com.server;

public abstract class Event implements Comparable<Event> {

    protected int sequenceNo = -1;  // default unused
    protected String id = "";       // default unused

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