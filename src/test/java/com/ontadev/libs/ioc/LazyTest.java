package com.ontadev.libs.ioc;

import com.ontadev.libs.ioc.annotation.AutoListener;
import com.ontadev.libs.ioc.annotation.common.Lazy;
import com.ontadev.libs.ioc.annotation.injection.Inject;
import com.ontadev.libs.ioc.annotation.lifecycle.PostConstruct;
import com.ontadev.libs.ioc.annotation.lifecycle.Shutdown;
import com.ontadev.libs.ioc.annotation.stereotype.Component;
import com.ontadev.libs.ioc.annotation.stereotype.Service;
import com.ontadev.libs.ioc.handlers.impl.ComponentHandler;
import com.ontadev.libs.ioc.handlers.impl.InjectFieldHandler;
import com.ontadev.libs.ioc.handlers.impl.PostConstructHandler;
import com.ontadev.libs.ioc.handlers.impl.ServiceHandler;
import com.ontadev.libs.ioc.handlers.impl.ShutdownHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LazyTest {
    static final List<String> events = new ArrayList<>();
    private IoCContainer container;
    private ShutdownHandler shutdown;

    @BeforeEach
    void setUp() {
        events.clear();
        container = new IoCContainer();
        container.registerClassHandler(new ComponentHandler());
        container.registerClassHandler(new ServiceHandler());
        container.registerFieldHandler(new InjectFieldHandler());
        container.registerMethodHandler(new PostConstructHandler());
        shutdown = new ShutdownHandler();
        container.registerMethodHandler(shutdown);
    }

    public static class Dependency {
        boolean ready;
        @PostConstruct private void initialize() { ready = true; }
    }

    @Service @Lazy
    public static class LazyService {
        @Inject Dependency dependency;
        boolean ready;
        public LazyService() { events.add("construct"); }
        @PostConstruct private void initialize() {
            assertNotNull(dependency);
            ready = true;
            events.add("initialize");
        }
        @Shutdown private void shutdown() { events.add("shutdown"); }
    }

    @Component @Lazy
    public static class LazyComponent {
        public LazyComponent() { events.add("component"); }
    }

    @Component
    public static class ProviderHost {
        final Provider<LazyService> constructorProvider;
        @Inject Provider<LazyService> fieldProvider;
        public ProviderHost(Provider<LazyService> provider) { constructorProvider = provider; }
    }

    @Test
    void skipsUnusedLazyServicesAndComponentsIncludingShutdown() {
        container.initialize(Set.of(LazyService.class, LazyComponent.class));
        assertNull(container.getIfExists(LazyService.class));
        assertNull(container.getIfExists(LazyComponent.class));
        shutdown.runAll();
        assertTrue(events.isEmpty());
    }

    @Test
    void firstProviderAccessRunsFullLifecycleAndCachesSingleton() {
        container.initialize(Set.of(ProviderHost.class, LazyService.class, LazyComponent.class));
        ProviderHost host = container.get(ProviderHost.class);
        assertTrue(events.isEmpty());
        LazyService service = host.constructorProvider.get();
        assertTrue(service.ready);
        assertTrue(service.dependency.ready);
        assertSame(service, host.fieldProvider.get());
        assertSame(service, container.get(LazyService.class));
        assertEquals(List.of("construct", "initialize"), events);
        shutdown.runAll();
        assertEquals(List.of("construct", "initialize", "shutdown"), events);
        LazyComponent component = container.get(LazyComponent.class);
        assertSame(component, container.get(LazyComponent.class));
        assertEquals(1, events.stream().filter("component"::equals).count());
    }

    @Component
    public static class DirectHost {
        final LazyService service;
        public DirectHost(LazyService service) { this.service = service; }
    }

    @Test
    void directConstructorDependencyCreatesLazyBeanDuringStartup() {
        container.initialize(Set.of(DirectHost.class, LazyService.class));
        LazyService service = container.get(DirectHost.class).service;
        assertSame(service, container.get(LazyService.class));
        assertTrue(service.ready);
        assertNotNull(service.dependency);
    }

    @Component
    public static class DirectFieldHost {
        @Inject LazyService service;
    }

    @Test
    void directFieldDependencyCreatesBeanWithoutConcurrentModification() {
        container.initialize(Set.of(DirectFieldHost.class, LazyService.class));
        assertTrue(container.get(DirectFieldHost.class).service.ready);
        assertEquals(List.of("construct", "initialize"), events);
    }

    @Service @Lazy
    public interface LazyApi {}

    public static class LazyApiImpl implements LazyApi {
        public LazyApiImpl() { events.add("api"); }
        @Inject Dependency dependency;
        @PostConstruct private void initialize() { assertTrue(dependency.ready); }
    }

    @Test
    void supportsLazyAnnotatedServiceInterfaceAndImplementationAlias() {
        container.initialize(Set.of(LazyApi.class, LazyApiImpl.class));
        assertTrue(events.isEmpty());
        LazyApi api = container.get(LazyApi.class);
        assertSame(api, container.get(LazyApiImpl.class));
        assertEquals(List.of("api"), events);
    }

    @Component
    public static class LifecycleHost {
        final Provider<LazyService> provider;
        public LifecycleHost(Provider<LazyService> provider) { this.provider = provider; }
        @PostConstruct private void initialize() { assertTrue(provider.get().ready); }
    }

    @Test
    void supportsFirstAccessInsideAnotherPostConstruct() {
        container.initialize(Set.of(LifecycleHost.class, LazyService.class));
        assertTrue(container.get(LazyService.class).ready);
        assertTrue(container.get(LazyService.class).dependency.ready);
        assertEquals(List.of("construct", "initialize"), events);
    }

    @Service @Lazy
    public static class FailingService {
        @Inject Dependency dependency;
        @PostConstruct private void initialize() {
            assertTrue(dependency.ready);
            events.add("failed");
            throw new IllegalStateException("boom");
        }
        @Shutdown private void shutdown() { events.add("invalid shutdown"); }
    }

    @Test
    void failedPostConstructDoesNotCacheBeanOrRegisterShutdownAndCanRetry() {
        container.initialize(Set.of(FailingService.class));
        assertThrows(IllegalStateException.class, () -> container.get(FailingService.class));
        assertNull(container.getIfExists(FailingService.class));
        assertThrows(IllegalStateException.class, () -> container.get(FailingService.class));
        shutdown.runAll();
        assertEquals(List.of("failed", "failed"), events);
    }

    @Service @Lazy
    public static class CycleA {
        public CycleA(CycleB ignoredDependency) {}
    }
    @Service @Lazy
    public static class CycleB {
        public CycleB(CycleA ignoredDependency) {}
    }

    @Test
    void unusedLazyConstructorCycleIsDetectedOnlyOnAccess() {
        assertDoesNotThrow(() -> container.initialize(Set.of(CycleA.class, CycleB.class)));
        assertThrows(IllegalStateException.class, () -> container.get(CycleA.class));
        assertNull(container.getIfExists(CycleA.class));
        assertNull(container.getIfExists(CycleB.class));
    }

    @Component @Lazy @AutoListener
    public static class ListenerComponent {
        public ListenerComponent() { events.add("listener"); }
    }

    @Test
    void listenerComponentRemainsEager() {
        container.initialize(Set.of(ListenerComponent.class));
        assertEquals(List.of("listener"), events);
    }
}
