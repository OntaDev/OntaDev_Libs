// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers;

import com.ontadev.libs.ioc.IoCContainer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface MethodAnnotationHandler<A extends Annotation> {

    Class<A> getAnnotation();

    void handle(
            IoCContainer container,
            Object instance,
            Method method,
            A annotation
    );

}