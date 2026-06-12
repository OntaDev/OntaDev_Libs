// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.serialization.adapters;

import com.google.gson.*;
import com.ontadev.libs.serialization.GsonAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Type;

@GsonAdapter(Component.class)
public class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return legacySerializer.deserialize(json.getAsString());
    }

    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(legacySerializer.serialize(src));
    }
}
