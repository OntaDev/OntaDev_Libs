// OntaDev_Libs Plugin
// Авторские права (c) 2026 OntaDev
// Лицензия: MIT

package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.handlers.MethodAnnotationHandler;
import com.ontadev.libs.ioc.annotation.Shutdown;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ShutdownHandler implements MethodAnnotationHandler<Shutdown> {

    private final List<Runnable> shutdownTasks = new ArrayList<>();

    @Override
    public Class<Shutdown> getAnnotation() {
        return Shutdown.class;
    }

    @Override
    public void handle(IoCContainer container, Object instance, Method method, Shutdown annotation) {
        method.setAccessible(true);
        shutdownTasks.add(() -> {
            try {
                method.invoke(instance);
            } catch (Exception e) {
                log.error("Error while invoking method with message: {}", e.getMessage(), e);
            }
        });
    }

    public void runAll() {
        for (Runnable task : shutdownTasks) {
            task.run();
        }
    }
}