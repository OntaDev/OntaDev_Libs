// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.orm.annotation.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает автоматически генерируемое числовое поле.
 * Для первичного ключа включает автоинкремент; save() записывает полученный ключ в поле.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratedValue {
}
