// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.adapter;

import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.orm.annotation.entity.Entity;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;

import java.lang.reflect.Type;
import java.util.Optional;

@RequiredArgsConstructor
public final class EntityRowMapperFactory implements RowMapperFactory {
    private final EntityMapper entityMapper;

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        if (!(type instanceof Class<?>)) {
            return Optional.empty();
        }

        Class<?> entityClass = (Class<?>) type;

        if (Void.class.isAssignableFrom(entityClass)){
            return Optional.of((rs, ctx) -> null);
        }

        if (!entityClass.isAnnotationPresent(Entity.class)) {
            return Optional.empty();
        }

        return Optional.of((rs, ctx) -> entityMapper.mapRow(rs, entityClass, ctx));
    }
}