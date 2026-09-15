// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm;

public interface OrmRepository<T, ID> {
    <S extends T> S save(S instance);
}