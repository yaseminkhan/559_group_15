package com.server;

public class Chat extends Event {
    public String sender;
    public String text;
    public boolean correct = false;
    public double timestamp;

    public Chat() {}

    public Chat(String sender, String id, String text, boolean correct) {
        this.sender = sender;
        this.id = id;
        this.text = text;
        this.correct = correct;
    }

    public Chat(String sender, String id, String text) {
        this(sender, id, text, false);
    }

    public double getTimestamp() {
        return this.timestamp;
    }

    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "\nChat(sender=\"%s\", id=\"%s\", text=\"%s\", correct=%s)"
                .formatted(sender, id, text, correct);
    }
}