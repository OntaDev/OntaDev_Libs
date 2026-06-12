// OntaDev_Libs Plugin
// Авторские права (c) 2025 OntaDev
// Лицензия: MIT

package com.ontadev.libs.serialization.adapters;

import com.google.gson.*;
import com.ontadev.libs.serialization.GsonAdapter;
import org.bukkit.Particle;

import java.lang.reflect.Type;

@GsonAdapter(Particle.class)
public class ParticleAdapter implements JsonSerializer<Particle>, JsonDeserializer<Particle> {
    @Override
    public Particle deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonPrimitive()) {
            return null;
        }
        return Particle.valueOf(json.getAsString());
    }

    @Override
    public JsonElement serialize(Particle src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.name());
    }
}
