package com.ontadev.libs.ioc.annotation.lifecycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Вызывает метод после внедрения полей при инициализации IoC.
 * Для ленивого бина выполняется при первом запросе, после внедрения его полей.
 * Метод должен быть нестатическим, без аргументов и с результатом void.
 * Допускается private-доступ. Обрабатываются методы, объявленные в классе экземпляра.
 * Каждый метод вызывается один раз для экземпляра; порядок вызовов не задан.
 * Ошибка метода прерывает инициализацию контейнера или запрос ленивого бина.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {
}
