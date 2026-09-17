// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc;

@FunctionalInterface
public interface Provider<T> {
    T get();
}
