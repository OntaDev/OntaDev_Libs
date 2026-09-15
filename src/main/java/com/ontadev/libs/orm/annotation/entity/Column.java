package com.ontadev.libs.orm.annotation.entity;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /** Имя колонки. Пусто -> имя поля в lower_snake_case. */
    String value() default "";

    /** Явный SQL-тип, переопределяет автоопределение по java-типу. Пусто -> автоопределение. */
    String type() default "";

    boolean nullable() default true;

    /** Для строковых типов -> VARCHAR(length). Игнорируется для TEXT/других типов. */
    int length() default 255;

    /** Для DECIMAL. */
    int precision() default 19;

    int scale() default 4;

    boolean unique() default false;

    boolean indexed() default false;

    String indexName() default "";

    String defaultValue() default "";

    boolean defaultExpression() default false;

}