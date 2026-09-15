// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.mapper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class FieldMeta {
    private final Field field;

    private final Class<?> javaType;
    private final String sqlType;

    private final String columnName;
    private final boolean primaryKey;
    private final boolean autoIncrement;
    private final boolean nullable;
    private final boolean unique;
    private final int length;
    private final int precision;
    private final int scale;
    private final String defaultValue;
    private final boolean defaultExpression;
    private final boolean indexed;
    private final String indexName;

}