package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.common.Priority;
import com.ontadev.libs.ioc.annotation.stereotype.Command;
import com.ontadev.libs.ioc.handlers.ClassAnnotationHandler;
import com.ontadev.libs.ioc.handlers.FieldAnnotationHandler;
import com.ontadev.libs.ioc.handlers.InterfaceHandler;
import com.ontadev.libs.ioc.handlers.MethodAnnotationHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor
@SuppressWarnings({"unchecked", "unused"})
public class IoCContainer {

    private final Map<Class<? extends Annotation>, Integer> classHandlersPriority = new HashMap<>();
    private final Map<Class<? extends Annotation>, ClassAnnotationHandler<?>> classHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, FieldAnnotationHandler<?>> fieldHandlers = new HashMap<>();
    private final Map<Class<? extends Annotation>, MethodAnnotationHandler<?>> methodHandlers = new HashMap<>();
    private final Map<Class<?>, InterfaceHandler<?>> interfaceHandlerMap = new HashMap<>();

    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementations = new HashMap<>();
    private final Map<Class<?>, ComponentPriority> components = new HashMap<>();

    private final Set<Class<?>> blockedClasses = new HashSet<>();

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

    private void block(Object handler) {
        blockedClasses.add(handler.getClass());
    }

    private void handleByMarker(Class<?> iface, InterfaceHandler<?> rawHandler,
                                Map<Class<?>, List<Class<?>>> implementationsByInterface) {
        InterfaceHandler<Object> handler = (InterfaceHandler<Object>) rawHandler;

        Class<?> implementation = resolveSingleImplementation(iface, implementationsByInterface.get(iface));

        Object instance;

        if (implementation != null) {
            instance = handler.onImplementationFound(this, iface, implementation);

            if (instance == null) {
                registerImplementationRaw(iface, implementation);
                registerComponent(implementation, 0, null);
                return;
            }
        } else {
            instance = handler.createRealization(this, iface);

            if (instance == null) {
                log.warn("InterfaceHandler {} для {} не вернул реализацию",
                        handler.getClass().getName(), iface.getName());
                return;
            }
        }

        registerInstance((Class<Object>) iface, instance);
    }

    private void scanClassAnnotations(Set<Class<?>> classes) {
        Map<Class<?>, List<Class<?>>> implementationsByInterface = buildImplementationMap(classes);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotation()) continue;
            if (blockedClasses.contains(clazz)) continue;

            if (clazz.isInterface()) {
                Optional<InterfaceHandler<?>> handler = findInterfaceHandler(clazz);

                if (handler.isPresent()) {
                    handleByMarker(clazz, handler.get(), implementationsByInterface);
                    continue;
                }

                scanInterfaceAnnotations(clazz, implementationsByInterface);
                continue;
            }

            if (Modifier.isAbstract(clazz.getModifiers())) continue;

            for (Annotation annotation : clazz.getAnnotations()) {
                findHandler(classHandlers, annotation)
                        .map(h -> ((ClassAnnotationHandler<Annotation>) h))
                        .ifPresent(handler -> handler.handle(this, clazz, annotation));
            }
        }
    }

    private Map<Class<?>, List<Class<?>>> buildImplementationMap(Set<Class<?>> classes) {
        Map<Class<?>, List<Class<?>>> map = new HashMap<>();

        for (Class<?> candidate : classes) {
            if (candidate.isInterface() || candidate.isAnnotation() || Modifier.isAbstract(candidate.getModifiers()))
                continue;
            if (blockedClasses.contains(candidate)) continue;

            for (Class<?> iface : collectAllInterfaces(candidate)) {
                map.computeIfAbsent(iface, k -> new ArrayList<>()).add(candidate);
            }
        }

        return map;
    }

    private Set<Class<?>> collectAllInterfaces(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        Deque<Class<?>> stack = new ArrayDeque<>(Arrays.asList(clazz.getInterfaces()));

        while (!stack.isEmpty()) {
            Class<?> iface = stack.pop();
            if (!result.add(iface)) continue;
            stack.addAll(Arrays.asList(iface.getInterfaces()));
        }

        return result;
    }

    private void scanInterfaceAnnotations(Class<?> iface, Map<Class<?>, List<Class<?>>> implementationsByInterface) {
        for (Annotation annotation : iface.getAnnotations()) {

            Optional<ClassAnnotationHandler<?>> handlerOpt = findHandler(classHandlers, annotation);
            if (handlerOpt.isEmpty()) continue;

            Class<?> implementation = resolveSingleImplementation(iface, implementationsByInterface.get(iface));
            if (implementation == null) continue;

            registerImplementationRaw(iface, implementation);
            registerComponent(implementation, 0, annotation.annotationType());

            ClassAnnotationHandler<Annotation> handler = (ClassAnnotationHandler<Annotation>) handlerOpt.get();
            handler.handle(this, implementation, annotation);
        }
    }

    private Class<?> resolveSingleImplementation(Class<?> iface, List<Class<?>> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            log.debug("Реализация для интерфейса {} не найдена, пропускаю", iface.getName());
            return null;
        }

        if (candidates.size() > 1) {
            log.warn("Найдено несколько реализаций интерфейса {} ({}), пропускаю",
                    iface.getName(),
                    candidates.stream().map(Class::getName).collect(Collectors.joining(", ")));
            return null;
        }

        return candidates.get(0);
    }

    @SuppressWarnings("rawtypes")
    private void registerImplementationRaw(Class<?> iface, Class<?> impl) {
        registerImplementation((Class) iface, impl);

    }

    /*
     * Фаза создания instance
     */

    private void instantiateComponents() {
        for (Class<?> component : resolveCreationOrder()) {
            create(component);
        }
    }

    private List<Class<?>> resolveCreationOrder() {
        Map<Class<?>, List<Class<?>>> dependencyGraph = buildDependencyGraph();
        return topologicalSort(dependencyGraph);
    }

    private Map<Class<?>, List<Class<?>>> buildDependencyGraph() {
        Map<Class<?>, List<Class<?>>> graph = new HashMap<>();

        for (Class<?> component : components.keySet()) {
            List<Class<?>> dependencies = new ArrayList<>();

            Constructor<?> constructor;
            try {
                constructor = ConstructorResolver.resolve(component);
            } catch (Exception exception) {
                log.warn("Не удалось определить конструктор для {}, зависимость будет разрешена во время создания", component.getName());
                graph.put(component, dependencies);
                continue;
            }

            for (Class<?> paramType : constructor.getParameterTypes()) {
                Class<?> resolved = implementations.getOrDefault(paramType, paramType);
                if (!resolved.equals(component) && components.containsKey(resolved)) {
                    dependencies.add(resolved);
                }
            }

            graph.put(component, dependencies);
        }

        return graph;
    }

    private List<Class<?>> topologicalSort(Map<Class<?>, List<Class<?>>> graph) {
        Map<Class<?>, Integer> inDegree = new HashMap<>();
        Map<Class<?>, List<Class<?>>> dependents = new HashMap<>();

        for (Class<?> node : graph.keySet()) {
            inDegree.putIfAbsent(node, 0);
            dependents.putIfAbsent(node, new ArrayList<>());
        }

        for (Map.Entry<Class<?>, List<Class<?>>> entry : graph.entrySet()) {
            Class<?> node = entry.getKey();
            for (Class<?> dependency : entry.getValue()) {
                dependents.computeIfAbsent(dependency, k -> new ArrayList<>()).add(node);
                inDegree.merge(node, 1, Integer::sum);
            }
        }

        Comparator<Class<?>> priorityComparator = (c1, c2) -> {
            ComponentPriority p1 = components.get(c1);
            ComponentPriority p2 = components.get(c2);
            int compare = Integer.compare(p2.annotationPriority(), p1.annotationPriority());
            if (compare != 0) return compare;
            return Integer.compare(p2.priority(), p1.priority());
        };

        PriorityQueue<Class<?>> ready = new PriorityQueue<>(priorityComparator);
        for (Map.Entry<Class<?>, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) ready.add(entry.getKey());
        }

        List<Class<?>> result = new ArrayList<>();

        while (!ready.isEmpty()) {
            Class<?> node = ready.poll();
            result.add(node);

            for (Class<?> dependent : dependents.getOrDefault(node, List.of())) {
                if (inDegree.merge(dependent, -1, Integer::sum) == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (result.size() != graph.size()) {
            List<String> unresolved = graph.keySet().stream()
                    .filter(c -> !result.contains(c))
                    .map(Class::getName)
                    .collect(Collectors.toList());
            throw new IllegalStateException("Обнаружена циклическая зависимость среди компонентов: " + unresolved);
        }

        return result;
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
        block(handler);
    }

    public void registerFieldHandler(FieldAnnotationHandler<?> handler) {
        fieldHandlers.put(handler.getAnnotation(), handler);
        block(handler);
    }

    public void registerMethodHandler(MethodAnnotationHandler<?> handler) {
        methodHandlers.put(handler.getAnnotation(), handler);
        block(handler);
    }

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

    public void registerInterfaceHandler(InterfaceHandler<?> handler) {
        interfaceHandlerMap.put(handler.getMarkerInterface(), handler);
        block(interfaceHandlerMap);
    }

    /*
     * Create instance
     */

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz) {
        log.debug("Creating instance of {}", clazz.getName());
        Object existing = instances.get(clazz);

        if (existing != null) {
            return (T) existing;
        }

        if (implementations.containsKey(clazz)) {
            Class<? extends T> impl = (Class<? extends T>) implementations.get(clazz);
            log.debug("Implementation already exists for {} creating {}", clazz.getName(), impl.getName());
            return create(impl);
        }

        if (clazz.isAnnotation() || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            log.error("Class is {} is interface/annotation/abstract", clazz.getName());
            return null;
        }

        T instance = preConstruct(clazz);

        if (instance != null) {
            instances.put(clazz, instance);
            postConstruct(clazz, instance);
            return instance;
        }

        try {
            var constructor = ConstructorResolver.resolve(clazz);

            Type[] genericParams = constructor.getGenericParameterTypes();

            Object[] params = Arrays.stream(genericParams)
                    .map(this::resolveDependency)
                    .toArray();

            instance = (T) constructor.newInstance(params);
            instances.put(clazz, instance);

            postConstruct(clazz, instance);

            return instance;

        } catch (Exception exception) {
            throw new RuntimeException("Failed create " + clazz.getName(), exception);
        }
    }

    /**
     * Резолвит generic-тип параметра/поля: {@code Provider<X>} превращается в отложенный
     * {@code () -> get(X.class)} вместо немедленного создания X, любой другой тип
     * резолвится как раньше — через {@link #get(Class)}.
     * <p>
     * Публичный, а не приватный, потому что нужен и {@code IoCContainer.create()},
     * и {@code InjectFieldHandler} (другой пакет) — одна реализация на оба места.
     */
    public Object resolveDependency(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            if (parameterizedType.getRawType() == Provider.class) {
                Class<?> actual = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                return (Provider<?>) () -> this.get(actual);
            }
        }

        return get((Class<?>) type);
    }

    private <T> T preConstruct(Class<T> clazz) {
        log.debug("Constructing instance of {}", clazz.getName());

        T instance = null;

        for (Annotation annotation : clazz.getAnnotations()) {
            ClassAnnotationHandler<Annotation> annotationHandler = (ClassAnnotationHandler<Annotation>) findHandler(classHandlers, annotation).orElse(null);

            if (annotationHandler != null) {
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
        for (Annotation annotation : clazz.getAnnotations()) {
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

    private Optional<InterfaceHandler<?>> findInterfaceHandler(Class<?> clazz) {
        for (Class<?> inter : collectAllInterfaces(clazz)) {
            InterfaceHandler<?> handler = interfaceHandlerMap.get(inter);
            if (handler != null) {
                return Optional.of(handler);
            }
        }
        return Optional.empty();
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

    @Accessors(fluent = true)
    @Getter
    @RequiredArgsConstructor
    private static class ComponentPriority {
        private final int priority;
        private final int annotationPriority;
        private final Class<? extends Annotation> annotation;

        private static ComponentPriority defaultPriority() {
            return new ComponentPriority(0, 0, Command.class);
        }
    }
}