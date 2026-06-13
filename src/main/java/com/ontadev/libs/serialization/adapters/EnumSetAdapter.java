// OntaDev_Libs Plugin
// Copyright (c) 2026 OntaDev
// License: MIT

package com.ontadev.libs.serialization.adapters;

import com.google.gson.*;
import com.ontadev.libs.serialization.GsonAdapter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.EnumSet;

@GsonAdapter(EnumSet.class)
public class EnumSetAdapter implements JsonSerializer<EnumSet<?>>, JsonDeserializer<EnumSet<?>> {

    @Override
    public EnumSet<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Class<?> rawEnumClass = resolveEnumClass(typeOfT);
        if (rawEnumClass == null || !rawEnumClass.isEnum()) {
            throw new JsonParseException("EnumSet element type is not an enum: " + typeOfT);
        }
        return deserializeEnumSet(rawEnumClass.asSubclass(Enum.class), json, context);
    }

    @Override
    public JsonElement serialize(EnumSet<?> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        if (src == null) {
            return array;
        }
        for (Object value : src) {
            array.add(context.serialize(value));
        }
        return array;
    }

    private Class<?> resolveEnumClass(Type typeOfT) {
        if (typeOfT instanceof ParameterizedType) {
            Type arg = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
            if (arg instanceof Class<?>) {
                return (Class<?>) arg;
            }
        }
        return null;
    }

    private <E extends Enum<E>> EnumSet<E> deserializeEnumSet(
            Class<E> enumClass,
            JsonElement json,
            JsonDeserializationContext context
    ) {
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        if (json == null || !json.isJsonArray()) {
            return set;
        }

        for (JsonElement element : json.getAsJsonArray()) {
            try {
                E value = context.deserialize(element, enumClass);
                if (value != null) {
                    set.add(value);
                }
            } catch (JsonParseException ignored) {
            }
        }

        return set;
    }
}
