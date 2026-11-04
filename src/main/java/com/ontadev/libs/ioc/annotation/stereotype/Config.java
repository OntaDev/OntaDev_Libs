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
 * Регистрирует конфиг как компонент IoC-контейнера.
 * Класс должен наследовать YamlConfig; экземпляр создаётся через YamlConfigLoader.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Priority(priority = 103)
public @interface Config {
    /**
     * Приоритет среди компонентов с одинаковым приоритетом аннотации.
     * Большие значения создаются раньше при соблюдении порядка зависимостей.
     */
    int priority() default 0;
}