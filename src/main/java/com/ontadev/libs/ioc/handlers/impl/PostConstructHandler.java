package com.ontadev.libs.ioc.handlers.impl;

import com.ontadev.libs.ioc.IoCContainer;
import com.ontadev.libs.ioc.annotation.lifecycle.PostConstruct;
import com.ontadev.libs.ioc.handlers.MethodAnnotationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Вызывает методы PostConstruct на фазе обработки методов IoC. */
public final class PostConstructHandler implements MethodAnnotationHandler<PostConstruct> {
    private final Map<Object, Set<Method>> invoked = new IdentityHashMap<>();

    @Override
    public Class<PostConstruct> getAnnotation() {
        return PostConstruct.class;
    }

    @Override
    public void handle(IoCContainer container, Object instance, Method method, PostConstruct annotation) {
        if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 0
                || method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@PostConstruct requires a non-static void method without arguments: "
                    + method);
        }
        Set<Method> methods = invoked.computeIfAbsent(instance, key -> new HashSet<>());
        if (methods.contains(method)) return;
        try {
            method.setAccessible(true);
            method.invoke(instance);
            methods.add(method);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed @PostConstruct: " + method, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke @PostConstruct: " + method, e);
        }
    }
}
