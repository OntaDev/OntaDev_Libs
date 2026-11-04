// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.annotation.entity;

import java.lang.annotation.*;

/**
 * Включает отображение строк SQL-результата на класс через EntityRowMapperFactory.
 * Классу нужны конструктор без аргументов и одно поле с @Id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Entity {
}