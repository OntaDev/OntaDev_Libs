package com.ontadev.libs.ioc.annotation.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Задаёт приоритет обработчика аннотации в IoC-контейнере.
 * Большие значения обрабатываются раньше при соблюдении порядка зависимостей.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Priority {
    /**
     * Приоритет аннотации; большие значения обрабатываются раньше.
     */
    int priority() default 0;
}