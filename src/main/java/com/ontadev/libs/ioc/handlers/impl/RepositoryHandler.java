// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.Repository;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;

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