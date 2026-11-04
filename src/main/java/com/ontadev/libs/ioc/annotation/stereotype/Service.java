// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.annotation.stereotype;

import com.ontadev.libs.ioc.annotation.common.Priority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Регистрирует класс сервиса как компонент IoC-контейнера.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Priority(priority = 100)
public @interface Service {
    /**
     * Приоритет среди компонентов с одинаковым приоритетом аннотации.
     * Большие значения создаются раньше при соблюдении порядка зависимостей.
     */
    int priority() default 0;
}