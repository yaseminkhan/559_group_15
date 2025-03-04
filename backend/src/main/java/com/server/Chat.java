package com.server;

public class Chat {
    // Will change to private.

    public String sender;
    public String id;
    public String text;
    public boolean correct = false;
    public double timestamp;

    public Chat(String sender, String id, String text, boolean correct, double timestamp) {
        this.sender = sender;
        this.text = text;
        this.correct = correct;
        this.timestamp = timestamp;
    }

    public Chat(String sender, String id, String text, double timestamp) {
        this(sender, id, text, false, timestamp);
    }

    @Override
    public String toString() {
        return "Chat(sender=%s, id=%s, text=%s, correct=%s, timestamp=%f)".formatted(sender, id, text, correct, timestamp);
    }

}
