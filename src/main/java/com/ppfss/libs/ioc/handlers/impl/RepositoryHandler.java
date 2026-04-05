// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.Repository;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;

public class RepositoryHandler implements ClassAnnotationHandler<Repository> {

    @Override
    public Class<Repository> getAnnotation() {
        return Repository.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Repository annotation) {
        int priority = annotation.priority();
        container.registerComponent(clazz, priority, annotation.annotationType());
    }

}