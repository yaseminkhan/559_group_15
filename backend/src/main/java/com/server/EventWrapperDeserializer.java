package com.server;

import com.google.gson.*;
import java.lang.reflect.Type;

public class EventWrapperDeserializer implements JsonDeserializer<EventWrapper> {
    @Override
    public EventWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("type").getAsString();
        JsonElement dataElement = obj.get("data");

        Event data;
        switch (type) {
            case "CHAT":
                data = context.deserialize(dataElement, Chat.class);
                break;
            case "CANVAS":
                data = context.deserialize(dataElement, Game.CanvasUpdate.class);
                break;
            case "CLEAR":
                data = context.deserialize(dataElement, CanvasClear.class); // <--- Add this line
                break;
            default:
                throw new JsonParseException("Unknown event type: " + type);
        }

        return new EventWrapper(type, data);
    }
}