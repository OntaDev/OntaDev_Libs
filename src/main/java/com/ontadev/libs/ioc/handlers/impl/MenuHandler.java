// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.menu.Menu;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;
import com.ontadev.libs.menu.AbstractMenu;
import com.ontadev.libs.menu.manager.MenuManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuHandler implements ClassAnnotationHandler<Menu> {
    private final MenuManager menuManager;

    public MenuHandler(MenuManager menuManager){
        this.menuManager = menuManager;
    }

    @Override
    public Class<Menu> getAnnotation() {
        return Menu.class;
    }

    @Override
    public Object preCreate(IoCContainer container, Class<?> clazz, Menu annotation) {

        if (!clazz.isAssignableFrom(AbstractMenu.class)) {
            throwError(clazz.getName());
            return null;
        }

        return ClassAnnotationHandler.super.preCreate(container, clazz, annotation);
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, Menu annotation) {
        int priority = annotation.priority();

        container.registerComponent(clazz, priority, annotation.annotationType());
    }


    @Override
    public void postCreate(IoCContainer container, Object instance, Menu annotation) {
        if (!(instance instanceof AbstractMenu)) {
            throwError(instance.getClass().getName());
            return;
        }

        menuManager.registerMenu((AbstractMenu) instance);
    }

    private void throwError(String className){
        log.error("Class {} doesn't extend AbstractMenu", className);
        throw new RuntimeException(String.format("Class %s doesn't extend AbstaractMenu", className));
    }
}
