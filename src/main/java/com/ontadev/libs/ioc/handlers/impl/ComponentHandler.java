// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.Component;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;

public class ComponentHandler implements ClassAnnotationHandler<Component> {

    @Override
    public Class<Component> getAnnotation() {
        return Component.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Component annotation) {
        int priority = annotation.priority();
        container.registerComponent(clazz, priority, annotation.annotationType());
    }

}