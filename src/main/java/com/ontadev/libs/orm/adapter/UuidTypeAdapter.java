// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.adapter;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.UUID;

/**
 * Схема всегда хранит UUID как VARCHAR(36) ({@link com.ontadev.libs.orm.mapper.EntityMapper#getSqlType}),
 * а не нативный uuid-тип — поэтому биндим/читаем его как строку одинаково на всех диалектах,
 * не полагаясь на разное поведение JDBC-драйверов для setObject(UUID).
 */
public final class UuidTypeAdapter implements TypeAdapter {

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (type != UUID.class) {
            return Optional.empty();
        }
        return Optional.of(new StringArgument(value == null ? null : value.toString()));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (type != UUID.class) {
            return Optional.empty();
        }
        return Optional.of((ColumnMapper<UUID>) (rs, columnNumber, ctx) -> {
            String raw = rs.getString(columnNumber);
            return raw == null ? null : UUID.fromString(raw);
        });
    }
}
