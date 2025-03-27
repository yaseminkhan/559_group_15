package com.server;

public class Event implements Sequential {
    private String type;
    private Sequential message;
    private int sequenceNo;

    public Event(String type, Sequential message) {
        this.type = type;
        this.message = message;
    }

    public Sequential getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }

    public void setSequenceNumber(int val) {
        sequenceNo = val;
    }

    public int compareTo(Sequential s) {
        return Integer.compare(sequenceNo, s.getSequenceNumber());
    }
}
