// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.mapper;

import com.ontadev.libs.exception.InvalidEntityException;
import com.ontadev.libs.exception.UnsupportedDatabaseFeatureException;
import com.ontadev.libs.orm.annotation.entity.Column;
import com.ontadev.libs.orm.annotation.entity.GeneratedValue;
import com.ontadev.libs.orm.annotation.entity.Id;
import com.ontadev.libs.orm.annotation.entity.Table;
import com.ontadev.libs.orm.database.Dialect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.statement.StatementContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public final class EntityMapper {

    private final ConcurrentHashMap<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();
    private final Dialect dialect;

    public <T> T mapRow(ResultSet rs, Class<T> entityClass, StatementContext ctx) {

        EntityMetadata metadata = metadataOf(entityClass);

        try {
            T instance = entityClass.cast(metadata.constructor().newInstance());

            for (FieldMeta fieldMeta : metadata.fields()) {
                int columnIndex;
                try {
                    columnIndex = rs.findColumn(fieldMeta.columnName());
                } catch (SQLException missingColumn) {
                    continue; // колонки нет в текущем SELECT — пропускаем поле
                }

                ColumnMapper<?> mapper = ctx.findColumnMapperFor(fieldMeta.field().getType())
                        .orElseThrow(() -> new NoSuchMapperException(
                                "Не найден ColumnMapper для типа " + fieldMeta.field().getType().getName()
                                        + " (поле " + entityClass.getSimpleName() + "." + fieldMeta.field().getName() + ")"));

                Object value = mapper.map(rs, columnIndex, ctx);
                fieldMeta.field().set(instance, value);
            }

            return instance;
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException("Не удалось смапить строку в " + entityClass.getName(), exception);
        } catch (SQLException exception) {
            throw new RuntimeException("Ошибка чтения ResultSet при мапинге " + entityClass.getName(), exception);
        }
    }


    public String resolveTableName(Class<?> entityClass) {
        return metadataOf(entityClass).tableName();
    }

    public String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.value().isEmpty()) return column.value();
        return toSnakeCase(field.getName());
    }

    public String resolveIdColumn(Class<?> entityClass) {
        return metadataOf(entityClass).idColumnName();
    }

    public boolean isAutoIncrementId(Class<?> entityClass) {
        return metadataOf(entityClass).idAutoIncrement();
    }

    public Object resolveIdValue(Object entity) {
        EntityMetadata metadata = metadataOf(entity.getClass());
        try {
            return metadata.idField().get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void setIdValue(Object entity, Object idValue) {
        EntityMetadata metadata = metadataOf(entity.getClass());
        try {
            Class<?> idType = metadata.idField().getType();
            metadata.idField().set(entity, coerceIdValue(idValue, idType));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object coerceIdValue(Object idValue, Class<?> targetType) {
        if (idValue == null) return null;
        if (targetType.isInstance(idValue)) return idValue;

        if (idValue instanceof Number) {
            Number number = (Number) idValue;
            if (targetType == Long.class || targetType == long.class) return number.longValue();
            if (targetType == Integer.class || targetType == int.class) return number.intValue();
        }

        return idValue;
    }

    public List<String> resolveInsertableColumns(Class<?> entityClass) {
        EntityMetadata metadata = metadataOf(entityClass);
        List<String> columns = new ArrayList<>(metadata.fields().size());
        for (FieldMeta fieldMeta : metadata.fields()) {
            if (fieldMeta.field() != metadata.idField()) {
                columns.add(fieldMeta.columnName());
            }
        }
        return Collections.unmodifiableList(columns);
    }

    public List<Object> resolveInsertableValues(Object entity) {
        EntityMetadata metadata = metadataOf(entity.getClass());
        List<Object> values = new ArrayList<>(metadata.fields().size());
        try {
            for (FieldMeta fieldMeta : metadata.fields()) {
                if (fieldMeta.field() != metadata.idField()) {
                    values.add(fieldMeta.field().get(entity));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return values;
    }

    public <T> T newInstance(Class<T> entityClass) {
        EntityMetadata metadata = metadataOf(entityClass);
        try {
            return entityClass.cast(metadata.constructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Не удалось создать экземпляр " + entityClass.getName(), e);
        }
    }

    public void warmUp(Class<?> entityClass) {
        metadataOf(entityClass);
    }


    public EntityMetadata metadataOf(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, this::buildMetadata);
    }

    private EntityMetadata buildMetadata(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        String tableName = table != null && !table.value().isEmpty()
                ? table.value()
                : toSnakeCase(entityClass.getSimpleName());

        Constructor<?> constructor;
        try {
            constructor = entityClass.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "У сущности " + entityClass.getName() + " нет no-arg конструктора", e);
        }

        List<FieldMeta> fields = new ArrayList<>();
        Field idField = null;
        String idColumnName = null;
        boolean idAutoIncrement = false;

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);
            String columnName = resolveColumnName(field);

            boolean autoIncrement = field.isAnnotationPresent(GeneratedValue.class);

            Column columnAnnotation = field.getAnnotation(Column.class);
            Id id = field.getAnnotation(Id.class);

            if (columnAnnotation == null && id == null) continue;

            String sqlType = getSqlType(field, columnAnnotation);

            if (id != null) {
                if (idColumnName != null) {
                    throw new InvalidEntityException(
                            String.format("Entity %s cannot have multiple primary keys", entityClass.getSimpleName())
                    );
                }

                idAutoIncrement = autoIncrement;
                idField = field;
                idColumnName = columnName;
            } else if (autoIncrement && !dialect.supportMultiAutoIncrement()) {
                throw new UnsupportedDatabaseFeatureException(
                        "The current database dialect does not support multiple auto-increment columns"
                );
            }

            fields.add(new FieldMeta(
                    field,
                    field.getType(),
                    sqlType,
                    columnName,
                    id != null,
                    autoIncrement,
                    columnAnnotation == null || columnAnnotation.nullable(),
                    columnAnnotation != null && columnAnnotation.unique(),
                    columnAnnotation != null ? columnAnnotation.length() : 255,
                    columnAnnotation != null ? columnAnnotation.precision() : 19,
                    columnAnnotation != null ? columnAnnotation.scale() : 4,
                    columnAnnotation != null ? columnAnnotation.defaultValue() : "",
                    columnAnnotation != null && columnAnnotation.defaultExpression(),
                    columnAnnotation != null && columnAnnotation.indexed(),
                    columnAnnotation != null ? columnAnnotation.indexName() : ""
            ));

        }

        if (idField == null) {
            throw new IllegalStateException("Не найдено поле @Id в " + entityClass.getName());
        }

        return new EntityMetadata(
                entityClass,
                tableName,
                constructor,
                Collections.unmodifiableList(fields),
                idField,
                idColumnName,
                idAutoIncrement
        );
    }

    private String getSqlType(Field field, Column column) {
        if (column != null && !column.type().isEmpty()) {
            return column.type();
        }

        Class<?> type = field.getType();

        if (type == String.class) {
            int length = column != null ? column.length() : 255;
            return length > 0 && length <= 65535 ? dialect.varchar(length) : dialect.text();
        }
        if (type == int.class || type == Integer.class) return dialect.integer();
        if (type == long.class || type == Long.class) return dialect.bigint();
        if (type == boolean.class || type == Boolean.class) return dialect.bool();
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) return dialect.doubleType();
        if (type == BigDecimal.class) {
            int precision = column != null ? column.precision() : 19;
            int scale = column != null ? column.scale() : 4;
            return dialect.decimal(precision, scale);
        }
        if (type == UUID.class) return dialect.varchar(36);
        if (type.isEnum()) return dialect.varchar(64);

        throw new IllegalArgumentException("Не указан @Column(type=...) для неизвестного типа поля: "
                + field.getName() + " (" + type.getName() + ")");
    }

    private static String toSnakeCase(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }


}