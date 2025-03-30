package com.server;

public abstract class Event implements Comparable<Event> {

    public abstract String getId();
    public abstract int getSequenceNumber();
    public abstract void setSequenceNumber(int val);
    
    public int compareTo(Event e) {
        if (e.getSequenceNumber() == getSequenceNumber()) {
            return getId().compareTo(e.getId());
        }
        return Integer.compare(getSequenceNumber(), e.getSequenceNumber());
    }

}
