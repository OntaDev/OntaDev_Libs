// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.Service;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;

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