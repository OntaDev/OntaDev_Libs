// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.serialization.adapters;

import com.google.gson.*;
import com.ontadev.libs.serialization.GsonAdapter;
import org.bukkit.Material;

import java.lang.reflect.Type;

@GsonAdapter(Material.class)
public class MaterialAdapter implements JsonSerializer<Material>, JsonDeserializer<Material> {

    @Override
    public Material deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonPrimitive()){
            return null;
        }

        return Material.getMaterial(json.getAsJsonPrimitive().getAsString().toUpperCase());
    }

    @Override
    public JsonElement serialize(Material src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.name());
    }
}
