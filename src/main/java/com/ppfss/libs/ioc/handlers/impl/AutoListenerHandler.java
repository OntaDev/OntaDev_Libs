// PPFSS_Libs Plugin 
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc.handlers.impl;

import com.ppfss.libs.ioc.IoCContainer;
import com.ppfss.libs.ioc.annotation.AutoListener;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Set;

@Slf4j
public class AutoListenerHandler implements ClassAnnotationHandler<AutoListener> {
    private final Set<Class<?>> listeners;

    public AutoListenerHandler(Set<Class<?>> listeners) {
        this.listeners = listeners;
    }

    @Override
    public Class<AutoListener> getAnnotation() {
        return AutoListener.class;
    }

    @Override
    public void handle(IoCContainer container, Class<?> clazz, AutoListener annotation) {
        if (!hasSubscribeMethods(clazz)) {
            log.warn("Class {} marked as @AutoListener but has no @Subscribe methods", clazz.getName());
            return;
        }

        listeners.add(clazz);
    }

    private boolean hasSubscribeMethods(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.velocitypowered.api.event.Subscribe.class)) {
                return true;
            }
        }
        return false;
    }
}