// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final Constructor<?> constructor;
    private final List<FieldMeta> fields;
    private final Field idField;
    private final String idColumnName;
    private final boolean idAutoIncrement;

}