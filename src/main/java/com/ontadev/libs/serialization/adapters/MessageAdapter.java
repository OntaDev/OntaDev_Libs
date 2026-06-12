// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.serialization.adapters;

import com.google.gson.*;
import com.ontadev.libs.message.Message;
import com.ontadev.libs.serialization.GsonAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@GsonAdapter(Message.class)
public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {
    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> lines = new ArrayList<>();

        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                lines.add(element.getAsString());
            }
        }else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            lines.add(json.getAsString());
        }else{
            throw new JsonParseException("Invalid JSON format");
        }

        return new Message(lines);
    }

    @Override
    public JsonElement serialize(Message message, Type typeOfSrc, JsonSerializationContext context) {
        List<String> lines = message.getRawMessage();
        if (lines == null || lines.isEmpty()) {
            return new JsonObject();
        }

        if (lines.size() == 1) {
            return new JsonPrimitive(lines.get(0));
        }else{
            JsonArray jsonArray = new JsonArray();
            for (String line : lines) {
                jsonArray.add(new JsonPrimitive(line));
            }
            return jsonArray;
        }
    }
}
