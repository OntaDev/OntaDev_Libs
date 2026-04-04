// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc;

import com.ppfss.libs.ioc.annotation.Component;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import com.ppfss.libs.ioc.handlers.FieldAnnotationHandler;
import com.ppfss.libs.ioc.handlers.MethodAnnotationHandler;
import com.ppfss.libs.ioc.handlers.impl.ComponentHandler;
import com.ppfss.libs.ioc.handlers.impl.InjectFieldHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@SuppressWarnings({"unchecked", "unused"})
public class IoCContainer {

    /*
     * Обработчики
     */

    private final Map<Class<? extends Annotation>, ClassAnnotationHandler<?>> classHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, FieldAnnotationHandler<?>> fieldHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, MethodAnnotationHandler<?>> methodHandlers = new HashMap<>();


    /*
     * Контекст (Объекты)
     */

    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();
    private final Set<Class<?>> components = new HashSet<>();


    public IoCContainer() {
        registerHandlers();
    }


    /*
     * Инициализация
     */

    public void initialize(Set<Class<?>> classes) {

        scanClassAnnotations(classes);

        instantiateComponents();

        injectFields();

        processMethods();
    }


    /*
     * Фаза сканирования
     */

    private void scanClassAnnotations(Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            boolean isComponent = false;

            for (Annotation annotation : clazz.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(Component.class)) {
                    isComponent = true;
                }

                findHandler(classHandlers, annotation)
                        .map(h -> ((ClassAnnotationHandler<Annotation>) h))
                        .ifPresent(handler -> handler.handle(this, clazz, annotation));
            }

            if (isComponent) {
                registerComponent(clazz);
            }
        }
    }


    /*
     * Фаза создания instance
     */

    private void instantiateComponents() {
        // сортируем по приоритету: больше — раньше
        List<Class<?>> sortedComponents = new ArrayList<>(components);

        sortedComponents.sort((c1, c2) -> {
            int p1 = getPriority(c1);
            int p2 = getPriority(c2);
            return Integer.compare(p2, p1); // higher priority first
        });

        for (Class<?> component : sortedComponents) {
            create(component);
        }
    }

    private int getPriority(Class<?> clazz) {
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                try {
                    Method m = ann.annotationType().getMethod("priority");
                    return (int) m.invoke(ann);
                } catch (NoSuchMethodException ignored) {
                } catch (Exception e) {
                    log.error("Failed get priority for " + clazz.getName(), e);
                }
            }
        }
        return 0;
    }




    /*
     * Фаза инъекций
     */

    private void injectFields() {

        for (Object instance : instances.values()) {

            Class<?> clazz = instance.getClass();

            for (Field field : clazz.getDeclaredFields()) {

                for (Annotation annotation : field.getAnnotations()) {

                    findHandler(fieldHandlers, annotation)
                            .map(h -> (FieldAnnotationHandler<Annotation>) h)
                            .ifPresent(h -> h.handle(this, instance, field, annotation));
                }
            }
        }
    }


    /*
     * Фаза обработки методов
     */

    private void processMethods() {

        for (Object instance : instances.values()) {

            for (Method method : instance.getClass().getDeclaredMethods()) {

                for (Annotation annotation : method.getAnnotations()) {

                    findHandler(methodHandlers, annotation)
                            .map(h -> (MethodAnnotationHandler<Annotation>) h)
                            .ifPresent(h -> h.handle(this, instance, method, annotation));

                }
            }
        }
    }


    /*
     * Handlers register
     */

    private void registerHandlers() {

        registerClassHandler(new ComponentHandler());

        registerFieldHandler(new InjectFieldHandler());

    }


    public void registerClassHandler(ClassAnnotationHandler<?> handler) {
        classHandlers.put(handler.getAnnotation(), handler);
    }

    public void registerFieldHandler(FieldAnnotationHandler<?> handler) {
        fieldHandlers.put(handler.getAnnotation(), handler);
    }

    public void registerMethodHandler(MethodAnnotationHandler<?> handler) {
        methodHandlers.put(handler.getAnnotation(), handler);
    }


    /*
     * Component registry
     */

    public void registerComponent(Class<?> clazz) {
        components.add(clazz);
    }

    public <T> void registerImplementation(Class<T> type, Class<? extends T> impl) {
        implementations.put(type, impl);
    }

    public <T> void registerInstance(Class<T> type, T instance) {
        instances.put(type, instance);
    }


    /*
     * Create instance
     */

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz) {
        Object existing = instances.get(clazz);

        if (existing != null) {
            return (T) existing;
        }

        try {
            var constructor = ConstructorResolver.resolve(clazz);

            Object[] params = Arrays.stream(constructor.getParameterTypes())
                    .map(this::get)
                    .toArray();

            T instance = (T) constructor.newInstance(params);
            instances.put(clazz, instance);
            return instance;

        } catch (Exception exception) {
            throw new RuntimeException("Failed create " + clazz.getName(), exception);
        }
    }


    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {

        Object instance = instances.get(type);

        if (instance != null) {
            return (T) instance;
        }

        Class<?> impl = implementations.get(type);

        if (impl != null) {
            return (T) create(impl);
        }

        return create(type);
    }

    @SuppressWarnings("unchecked")
    public <T> T getIfExists(Class<T> type) {
        Object instance = instances.get(type);
        if (instance != null) {
            return (T) instance;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <H> Optional<H> findHandler(Map<Class<? extends Annotation>, ?> handlers, Annotation annotation) {
        return Optional.ofNullable((H) handlers.get(annotation.annotationType()));
    }

    public Set<Class<?>> getAllClassesWithAnnotation(Class<? extends Annotation> annotation) {
        Set<Class<?>> result = new HashSet<>();
        for (Class<?> clazz : components) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }

}