package com.ontadev.libs.ioc.annotation.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Вызывает метод при инициализации IoC после внедрения полей всех экземпляров.
 * Метод должен быть нестатическим, без аргументов и с результатом void.
 * Допускается private-доступ. Обрабатываются методы, объявленные в классе экземпляра.
 * Каждый метод вызывается один раз для экземпляра; порядок вызовов не задан.
 * Ошибка метода прерывает инициализацию контейнера.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {
}
