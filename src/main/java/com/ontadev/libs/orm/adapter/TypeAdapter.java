// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.adapter;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;

public interface TypeAdapter extends ArgumentFactory, ColumnMapperFactory {
    default void registerOn(Jdbi jdbi) {
        jdbi.registerArgument(this);
        jdbi.registerColumnMapper(this);
    }
}