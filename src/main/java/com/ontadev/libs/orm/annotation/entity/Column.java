package com.ontadev.libs.orm.annotation.entity;

import java.lang.annotation.*;

/**
 * Включает поле в ORM-отображение и задаёт параметры SQL-колонки.
 * Параметры схемы используются при создании таблицы; существующая таблица не изменяется.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /**
     * Имя колонки; пустая строка использует имя поля в snake_case.
     */
    String value() default "";

    /**
     * Явный SQL-тип; пустая строка включает определение по Java-типу поля.
     */
    String type() default "";

    /**
     * Разрешает NULL. Для первичного ключа не применяется.
     */
    boolean nullable() default true;

    /**
     * Длина VARCHAR для String при автоматическом выборе типа.
     * Значения вне диапазона 1..65535 выбирают TEXT.
     */
    int length() default 255;

    /**
     * Общее число цифр DECIMAL для BigDecimal при автоматическом выборе типа.
     */
    int precision() default 19;

    /**
     * Число цифр после запятой DECIMAL для BigDecimal при автоматическом выборе типа.
     */
    int scale() default 4;

    /**
     * Добавляет UNIQUE при создании колонки, кроме первичного ключа.
     */
    boolean unique() default false;

    /**
     * Признак индекса в метаданных. Генератор таблиц индексы не создаёт.
     */
    boolean indexed() default false;

    /**
     * Имя индекса в метаданных. Генератор таблиц его не использует.
     */
    String indexName() default "";

    /**
     * Значение по умолчанию в метаданных. Генератор таблиц DEFAULT не добавляет.
     */
    String defaultValue() default "";

    /**
     * Признак SQL-выражения для defaultValue в метаданных.
     * Генератор таблиц его не использует.
     */
    boolean defaultExpression() default false;

}
