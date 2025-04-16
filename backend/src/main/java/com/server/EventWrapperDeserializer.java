package com.server;

import com.google.gson.*;
import java.lang.reflect.Type;

// Custom deserializer to convert JSON into EventWrapper objects
public class EventWrapperDeserializer implements JsonDeserializer<EventWrapper> {

    @Override
    public EventWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject(); // Convert the incoming JSON into a JsonObject
        String type = obj.get("type").getAsString(); // Extract the event type (e.g., CHAT, CANVAS, CLEAR)
        JsonElement dataElement = obj.get("data"); // Extract the event data payload

        Event data;

        // Deserialize the data field into the appropriate class based on the event type
        switch (type) {
            case "CHAT":
                data = context.deserialize(dataElement, Chat.class); // Deserialize as Chat
                break;
            case "CANVAS":
                data = context.deserialize(dataElement, Game.CanvasUpdate.class); // Deserialize as CanvasUpdate
                break;
            case "CLEAR":
                data = context.deserialize(dataElement, CanvasClear.class); // Deserialize as CanvasClear
                break;
            default:
                // Throw an error if an unknown event type is encountered
                throw new JsonParseException("Unknown event type: " + type);
        }

        // Wrap the deserialized object in an EventWrapper and return
        return new EventWrapper(type, data);
    }
}