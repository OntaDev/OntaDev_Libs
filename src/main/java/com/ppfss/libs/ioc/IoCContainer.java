// PPFSS_Libs Plugin
// Авторские права (c) 2026 PPFSS
// Лицензия: MIT

package com.ppfss.libs.ioc;

import com.ppfss.libs.ioc.annotation.Command;
import com.ppfss.libs.ioc.annotation.Priority;
import com.ppfss.libs.ioc.handlers.ClassAnnotationHandler;
import com.ppfss.libs.ioc.handlers.FieldAnnotationHandler;
import com.ppfss.libs.ioc.handlers.MethodAnnotationHandler;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
@SuppressWarnings({"unchecked", "unused"})
public class IoCContainer {

    /*
     * Обработчики
     */
    private final Map<Class<? extends Annotation>, Integer> classHandlersPriority = new HashMap<>();
    private final Map<Class<? extends Annotation>, ClassAnnotationHandler<?>> classHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, FieldAnnotationHandler<?>> fieldHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, MethodAnnotationHandler<?>> methodHandlers = new HashMap<>();


    /*
     * Контекст (Объекты)
     */

    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();
    private final Map<Class<?>, ComponentPriority> components = new HashMap<>();


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
            if (clazz.isAnnotation() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) continue;
            for (Annotation annotation : clazz.getAnnotations()) {
                findHandler(classHandlers, annotation)
                        .map(h -> ((ClassAnnotationHandler<Annotation>) h))
                        .ifPresent(handler -> handler.handle(this, clazz, annotation));
            }

        }
    }


    /*
     * Фаза создания instance
     */

    private void instantiateComponents() {
        List<Class<?>> classes = components.entrySet().stream()
                .sorted((c1, c2)->{
                    ComponentPriority priority1 = c1.getValue();
                    ComponentPriority priority2 = c2.getValue();

                    int compare = Integer.compare(priority2.annotationPriority(), priority1.annotationPriority());
                    if (compare != 0) return compare;

                    return Integer.compare(priority2.priority(), priority1.priority());

                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableList());



        for (Class<?> component : classes) {
            create(component);
        }
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


    public void registerClassHandler(ClassAnnotationHandler<?> handler) {
        int priority = 0;
        Priority priorityAn = handler.getAnnotation().getAnnotation(Priority.class);

        if (priorityAn != null) {
            priority = priorityAn.priority();
        }

        classHandlersPriority.put(handler.getAnnotation(), priority);
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
    @Deprecated
    public void registerComponent(Class<?> clazz) {
        components.put(clazz, ComponentPriority.defaultPriority());
    }

    public void registerComponent(Class<?> clazz, int priority, Class<? extends Annotation> annotation) {
        int annotationPriority = classHandlersPriority.getOrDefault(annotation, 0);
        components.put(clazz, new ComponentPriority(priority, annotationPriority, annotation));
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
        log.debug("Creating instance of " + clazz.getName());
        Object existing = instances.get(clazz);

        if (existing != null) {
            return (T) existing;
        }

        if (implementations.containsKey(clazz)) {
            Class<? extends T> impl = (Class<? extends T>) implementations.get(clazz);
            log.debug("Implementation already exists for {} creating {}", clazz.getName(), impl.getName());
            return create(impl);
        }

        if (clazz.isAnnotation() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())){
            log.error("Class is {} is interface/annotation/abstract", clazz.getName());
            return null;
        }

        T instance = preConstruct(clazz);

        if (instance != null){
            instances.put(clazz, instance);
            return instance;
        }

        try {
            var constructor = ConstructorResolver.resolve(clazz);

            Object[] params = Arrays.stream(constructor.getParameterTypes())
                    .map(this::get)
                    .toArray();

            instance = (T) constructor.newInstance(params);
            instances.put(clazz, instance);

            postConstruct(clazz, instance);

            return instance;

        } catch (Exception exception) {
            throw new RuntimeException("Failed create " + clazz.getName(), exception);
        }
    }

    private <T> T preConstruct(Class<T> clazz) {
        log.debug("Constructing instance of " + clazz.getName());

        T instance = null;

        for (Annotation annotation: clazz.getAnnotations()){
            ClassAnnotationHandler<Annotation> annotationHandler = (ClassAnnotationHandler<Annotation>) findHandler(classHandlers, annotation).orElse(null);

            if (annotationHandler != null){
                Object result = annotationHandler.preCreate(this, clazz, annotation);
                if (result != null) {
                    instance = (T) result;
                    break;
                }
            }
        }

        return instance;
    }

    private <T> void postConstruct(Class<T> clazz, T instance) {
        for (Annotation annotation: clazz.getAnnotations()){
            findHandler(classHandlers, annotation)
                    .map(h -> (ClassAnnotationHandler<Annotation>) h)
                    .ifPresent(handler -> handler.postCreate(this, instance, annotation));
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
        for (Class<?> clazz : components.keySet()) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }


    private record ComponentPriority(
      int priority,
      int annotationPriority,
      Class<? extends Annotation> annotation
    ){
        private static ComponentPriority defaultPriority(){
            return new ComponentPriority(0, 0, Command.class);
        }
    }
}