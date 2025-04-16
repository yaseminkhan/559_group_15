package com.server;

/**
 * Interface representing an object that can be assigned a sequence number
 * and compared based on that number.
 */
public interface Sequential extends Comparable<Sequential> {

    /**
     * Returns the sequence number of this object.
     * @return the current sequence number
     */
    public int getSequenceNumber();

    /**
     * Sets the sequence number of this object.
     * @param value the sequence number to set
     */
    public void setSequenceNumber(int value);
}