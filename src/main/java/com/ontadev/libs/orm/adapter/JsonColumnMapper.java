// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.adapter;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public class JsonColumnMapper<T> implements ColumnMapper<T> {
    private static final Gson GSON = new Gson();

    private final Class<T> clazz;

    @Override
    public T map(ResultSet r, int columnNumber, StatementContext ctx) throws SQLException {
        String json = r.getString(columnNumber);
        if (json == null || json.isBlank()){
            return null;
        }

        return GSON.fromJson(json, clazz);
    }
}
