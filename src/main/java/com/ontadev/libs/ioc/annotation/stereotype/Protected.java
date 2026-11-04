// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.annotation.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер класса с retention CLASS. IoC-контейнер его не обрабатывает.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Protected {
}
