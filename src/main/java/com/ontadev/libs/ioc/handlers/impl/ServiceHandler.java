// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;

public class ServiceHandler implements ClassAnnotationHandler<Service> {

    @Override
    public Class<Service> getAnnotation() {
        return Service.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Service annotation) {
        int priority = annotation.priority();
        container.registerComponent(clazz, priority, annotation.annotationType());
    }

}