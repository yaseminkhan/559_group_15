package com.server;

public interface Sequential extends Comparable<Sequential> {
    public int getSequenceNumber();
    public void setSequenceNumber(int value);
}
