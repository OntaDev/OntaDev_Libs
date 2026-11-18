package com.ontadev.libs.ioc.annotation.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Откладывает создание Service или Component до первого запроса из IoC.
 * Прямая зависимость создаёт бин при инициализации получателя; Provider откладывает запрос.
 * Повторные запросы возвращают тот же экземпляр после внедрения полей и обработки lifecycle-методов.
 * Сама аннотация не регистрирует класс как компонент.
 * На командах, слушателях, меню, конфигах и репозиториях аннотация не применяется.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Lazy {
}
