package com.server;

public class Chat implements Sequential {
    // Will change to private.

    public String sender;
    public String id;
    public String text;
    public boolean correct = false;
    public double timestamp;
    public int sequenceNo; // Sequence number for logical timestamps.

    public Chat(String sender, String id, String text, boolean correct, double timestamp, int sequenceNo) {
        this.sender = sender;
        this.id = id;
        this.text = text;
        this.correct = correct;
        this.timestamp = timestamp;
        this.sequenceNo = sequenceNo;
    }

    public Chat(String sender, String id, String text, double timestamp, int sequenceNo) {
        this(sender, id, text, false, timestamp, sequenceNo);
    }

    public int getSequenceNumber() {
        return sequenceNo;
    }

    public void setSequenceNumber(int value) {
        if (value < 0)
            throw new IllegalArgumentException("Can't assign a negative value to sequence numbers.");
        sequenceNo = value;
    }

    public int compareTo(Sequential message) {
        return Integer.compare(sequenceNo, message.getSequenceNumber());
    }

    @Override
    public String toString() {
        return "\nChat(sender=\"%s\", id=\"%s\", text=\"%s\", correct=%s, timestamp=%f, sequenceNo=%d)"
                .formatted(sender, id, text, correct, timestamp, sequenceNo);
    }

}
