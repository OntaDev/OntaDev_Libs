package com.ontadev.libs.orm.dto;

import com.ontadev.libs.orm.mapper.FieldMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class SaveContext {
    private final Field idField;
    private final List<FieldMeta> insertFields;
    private final List<FieldMeta> nonIdFields;
    private final String insertSql;
    private final String updateSql;
    private final String existsSql;

}