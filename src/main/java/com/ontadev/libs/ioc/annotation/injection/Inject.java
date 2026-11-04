// OntaDev_Libs Plugin 
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.annotation.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Выбирает публичный конструктор для создания IoC-компонента
 * или внедряет зависимость в поле. Зависимость ищется по точному типу.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface Inject {
}