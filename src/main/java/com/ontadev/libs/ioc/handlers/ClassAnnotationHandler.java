// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers;

import com.ontadev.libs.ioc.IoCContainer;

import java.lang.annotation.Annotation;

public interface ClassAnnotationHandler<A extends Annotation> {

    Class<A> getAnnotation();

    void handle(IoCContainer container, Class<?> clazz, A annotation);

    /**
     * Create: можно вернуть собственный объект вместо стандартного new.
     * Если возвращается null, IoC создаёт объект сам.
     */
    default Object preCreate(IoCContainer container, Class<?> clazz, A annotation) {
        return null;
    }

    /**
     * Post-create: вызывается после создания объекта.
     */
    default void postCreate(IoCContainer container, Object instance, A annotation) {}
}