// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.entity;

import com.ontadev.libs.orm.database.DatabaseManager;
import com.ontadev.libs.orm.database.Dialect;
import com.ontadev.libs.orm.mapper.EntityMapper;
import com.ontadev.libs.orm.mapper.EntityMetadata;
import com.ontadev.libs.orm.mapper.FieldMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public final class SchemaGenerator {
    private final DatabaseManager databaseManager;
    private final Set<Class<?>> alreadyGenerated = ConcurrentHashMap.newKeySet();
    private final EntityMapper entityMapper;

    public void ensureGenerated(Class<?> entityClass) {
        if (!alreadyGenerated.add(entityClass)) {
            return;
        }

        EntityMetadata metadata = entityMapper.metadataOf(entityClass);
        String ddl = buildCreateTable(databaseManager.getDialect(), metadata);

        try (Connection connection = databaseManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Схема применена для {} -> {} ({})",
                    entityClass.getSimpleName(), metadata.tableName(), databaseManager.getDialect());
        } catch (SQLException exception) {
            alreadyGenerated.remove(entityClass); // не запоминаем неудачную попытку
            throw new RuntimeException("Не удалось создать таблицу для "
                    + entityClass.getName() + ": " + ddl, exception);
        }
    }

    private String buildCreateTable(Dialect dialect, EntityMetadata metadata) {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(metadata.tableName()).append(" (");

        boolean first = true;
        for (FieldMeta fieldMeta : metadata.fields()) {
            if (!first) sql.append(", ");
            first = false;

            sql.append(fieldMeta.columnName()).append(' ');

            if (fieldMeta.primaryKey()) {
                sql.append(fieldMeta.autoIncrement()
                        ? dialect.autoIncrementPrimaryKey()
                        : fieldMeta.sqlType() + " PRIMARY KEY");
                continue;
            }

            sql.append(fieldMeta.sqlType());
            if (!fieldMeta.nullable()) sql.append(" NOT NULL");
            if (fieldMeta.unique()) sql.append(" UNIQUE");
        }

        sql.append(')');
        return sql.toString();
    }
}