package com.server;

// Chat event extending the base Event class
public class Chat extends Event {
    public String sender;         // Username of the person who sent the message
    public String text;           // The actual message content
    public boolean correct = false; // Whether the message was a correct guess
    public double timestamp;      // Optional timestamp for debugging or display (not used in ordering)

    public Chat() {}

    // Constructor with all fields
    public Chat(String sender, String id, String text, boolean correct) {
        this.sender = sender;
        this.id = id;             // Inherited from Event: typically user ID
        this.text = text;
        this.correct = correct;
    }

    // Constructor for normal chat messages (not marked correct)
    public Chat(String sender, String id, String text) {
        this(sender, id, text, false);
    }

    // Getter for timestamp (not used in event ordering)
    public double getTimestamp() {
        return this.timestamp;
    }

    // Overrides getId() to ensure compatibility with base Event class
    public String getId() {
        return this.id;
    }

    // For logging or debugging purposes
    @Override
    public String toString() {
        return "\nChat(sender=\"%s\", id=\"%s\", text=\"%s\", correct=%s)"
                .formatted(sender, id, text, correct);
    }
}