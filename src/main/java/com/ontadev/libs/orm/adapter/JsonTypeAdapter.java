// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.adapter;

import com.google.gson.Gson;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Резервный обработчик для полей, тип которых не покрыт ни jdbi3-core "из коробки"
 * (примитивы/wrapper-ы, String, BigDecimal, enum), ни {@link UuidTypeAdapter} —
 * такие поля (списки, map-ы, вложенные DTO) сериализуются в TEXT-колонку через Gson.
 * Для них обязателен явный {@code @Column(type = "TEXT")} — EntityMapper.getSqlType
 * не умеет угадывать SQL-тип для произвольных Java-типов.
 */
public final class JsonTypeAdapter implements TypeAdapter {
    private static final Gson GSON = new Gson();

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (!isJsonType(type)) {
            return Optional.empty();
        }
        return Optional.of(new StringArgument(value == null ? null : GSON.toJson(value)));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (!isJsonType(type)) {
            return Optional.empty();
        }
        return Optional.of(new JsonColumnMapper<>((Class<?>) type));
    }

    private static boolean isJsonType(Type type) {
        if (!(type instanceof Class<?>)) {
            return false;
        }
        return !isNativelySupported((Class<?>) type);
    }

    private static boolean isNativelySupported(Class<?> type) {
        // isEnum() is false for the runtime class of a constant with its own body (org.bukkit.ChatColor$16),
        // which is what JDBI hands us when a value is bound as Object - such a constant must still be
        // written as its plain name, not as Gson JSON ("\"WHITE\"").
        return type.isPrimitive()
                || Enum.class.isAssignableFrom(type)
                || type == String.class
                || type == Boolean.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == BigDecimal.class
                || type == UUID.class;
    }
}
